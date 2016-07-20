package restaurantfinder.example.tran.yelpfindrestaurants.controller;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import restaurantfinder.example.tran.yelpfindrestaurants.R;
import restaurantfinder.example.tran.yelpfindrestaurants.model.BusinessExtraInfo;
import restaurantfinder.example.tran.yelpfindrestaurants.model.BusinessObjects;
import restaurantfinder.example.tran.yelpfindrestaurants.model.BusinessObjectsDBHelper;
import restaurantfinder.example.tran.yelpfindrestaurants.model.Yelp;

/**
 * @author Todd
 * This class holds restaurant objects
 */
public class ListOfBOs {

	/**
	 * the sort mode for the search call.
	 */
	private String mSortValue;

	/**
	 * list of restaurant objects
	 */
	private List<BusinessObjects> mBusinessObjectsList;

	/**
	 * the number of results to be given back to the user
	 */
	private Integer mNumResults;
	
	/**
	 * the key words, i.e. pizza if the user is looking for places that serve pizza.
	 */
	private String mUserSearchTerm;

	/**
	 * object to make the yelp method calls
	 */
	private Yelp mYelp;

	/**
	 * A database object to help add to and read from the database.
	 */
	private BusinessObjectsDBHelper dbObject;

	/**
	 * The total number of businesses from the yelp search.
	 */
	private int mTotalBusinesses;

	/**
	 * Context of the current activity
	 */
	private Context mContext;
	
	/**
	 * initialize an empty list of restaurant objects
	 */
	public ListOfBOs(Context context)
	{
		this.mBusinessObjectsList = new ArrayList<>();
		mContext = context;
		Resources accessResources = mContext.getResources();
		mYelp = new Yelp(accessResources.getString(R.string.yelpAccessToken));
		dbObject = new BusinessObjectsDBHelper(context);
		mSortValue = "best_match"; // by default sort mode is best_match
	}
	
	/**
	 * initializes a list of restaurant objects.
	 * @param businessImage The image of the business.
	 * @param businessName The business's name.
	 * @param distance The distance from the business.
	 * @param businessType The number of reviews.
	 * @param businessStatus The status of the restaurant.
	 * @param businessLat The latitude (coordinate) of the business.
	 * @param businessLng The longitude (coordinate) of the business.
	 */
	public void addToListAndDB(Bitmap businessImage, String businessName, double distance, String businessType, String businessStatus, String businessLat, String businessLng)
	{
		BusinessObjects r1 = new BusinessObjects();
		r1.setBusinessImage(businessImage);
		r1.setBusinessName(businessName);
		r1.setDistance(distance);
		r1.setBusinessCategory(businessType);
		r1.setBusinessStatus(businessStatus);
		r1.setBusinessLat(businessLat);
		r1.setBusinessLng(businessLng);
		dbObject.addBusinessObject(r1);
		this.mBusinessObjectsList.add(r1);
	}

	/**
	 * sets the numResults field
	 * @param numResults the number of results
	 */
	public void setNumResults(Integer numResults)
	{
		this.mNumResults = numResults;
	}
	
	/**
	 * sets the search term field
	 * @param userSearchTerm the terms the user enters
	 */
	public void setUserSearchTerm(String userSearchTerm)
	{
		this.mUserSearchTerm = userSearchTerm;
	}

	/**
	 * sets the sort value field
	 * @param sortValue The sort value.
     */
	public void setSortValue(String sortValue) { mSortValue = sortValue; }


	/**
	 * this method uses the latitude and longitude from the calling Async task to update the contents of the lists of yelp search results
	 * one list will be sorted and the other will be sorted by highest review and the most reviews.
	 * @param latitude The user's current latitude (coordinate).
	 * @param longitude The user's current longitude (coordinate).
	 * @param offsetValue A value to determine which businesses get returned (first 20, next 20, etc).
	 */
	public void findBusinesses(double latitude, double longitude, int offsetValue)
	{
		// make the search.
		String response = mYelp.search(mUserSearchTerm, latitude, longitude, mNumResults, mSortValue, offsetValue); // based off the current address
		JSONObject json, business, businessLatLng, categoryObject;
		JSONArray businesses = null, categories;
		String businessName, businessLng, businessLat;
		double distanceToBusiness;
		Bitmap businessImage;
		String openStatus = "Open"; // with v3, there is a parameter when making the search call, open_now_filter, which is set to true so only open businesses are returned.
		try {
			json = new JSONObject(response);
			if(json != null) {
				mTotalBusinesses = json.getInt("total");
				businesses = json.getJSONArray("businesses");
				if(businesses.length() > 0) dbObject.deleteBusinessObjects();
			}
			for (int i = 0; i < businesses.length(); i++) {
				business = businesses.getJSONObject(i);
				businessName = business.getString("name");

				categories = business.getJSONArray("categories");
				StringBuilder categoryType = new StringBuilder();
				// go through the categories JSONArray.
				for(int j = 0; j < categories.length(); j++) {
					categoryObject = categories.getJSONObject(j);
					if(j == categories.length() - 1)
						categoryType.append(categoryObject.getString("title"));
					else
						categoryType.append(categoryObject.getString("title") + ", ");
				}

				businessLatLng = business.getJSONObject("coordinates");
				businessLng = String.valueOf(businessLatLng.getDouble("longitude"));
				businessLat = String.valueOf(businessLatLng.getDouble("latitude"));

				businessImage = getBusinessImage(business.getString("image_url")); // get an image of the business.

				distanceToBusiness = Double.parseDouble(getDistance(String.valueOf(latitude), String.valueOf(longitude), String.valueOf(businessLat), String.valueOf(businessLng)));

				addToListAndDB(businessImage, businessName, distanceToBusiness, categoryType.toString(), openStatus, businessLat, businessLng);
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @param currentLat The user's latitude.
	 * @param currentLng The user's longitude.
	 * @param destLat The latitude of the destination.
	 * @param destLng The longitude of the destination.
	 * @return The distance from the current location to the destination.
	 */
	public String getDistance(String currentLat, String currentLng, String destLat, String destLng) {
		String matrixAPIURL = "https://maps.googleapis.com/maps/api/distancematrix/json?units=imperial&origins=" + currentLat + "," + currentLng + "&destinations=" + destLat + "," + destLng + "&key=" + mContext.getResources().getString(R.string.GoogleServerKey);

		StringBuffer response = new StringBuffer();
		String destDistance = null;
		try {
			URL myURL = new URL(matrixAPIURL);
			HttpURLConnection myURLConnection = (HttpURLConnection)myURL.openConnection();

			BufferedReader in = new BufferedReader(new InputStreamReader(myURLConnection.getInputStream()));
			String inputLine;

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			JSONObject pageContents = new JSONObject(response.toString());
			JSONArray rowsArray = pageContents.getJSONArray("rows");

			JSONObject elementsObject = (JSONObject) rowsArray.get(0);
			JSONArray elementsArray = elementsObject.getJSONArray("elements");
			JSONObject distanceObject = (JSONObject) elementsArray.get(0);

			JSONObject myObject = distanceObject.getJSONObject("distance");
			destDistance = myObject.getString("text");
			// parse off the "mi" portion.
			String[] destinationDistance = destDistance.split(" ");
			if(destinationDistance != null && destinationDistance.length == 2 )
			{
				destDistance = destinationDistance[0];
			}
		} catch(ProtocolException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		} catch(JSONException e) {
			e.printStackTrace();
		}

		return destDistance;


	}

	/**
	 * @param restaurantPageURL The URL of the restaurant.
	 * @return A string to represent if the restaurant is currently open, closed, or soon to close.
	 */
	public BusinessExtraInfo getCurrentStatus(String restaurantPageURL) {
		Document doc;
		BusinessExtraInfo extraInformation = new BusinessExtraInfo();
		try {
			doc = Jsoup.connect(restaurantPageURL).get();
			String categoryContents = doc.select("li.category").text();
			String statusContents = doc.select("li.biz-hours.clearfix").text();
			// by default the data members have default values, they will only be overridden if values can be extracted from the business's detailed page.
			if(categoryContents != null && categoryContents.length() > 0) extraInformation.setBusinessCategory(categoryContents);
			if(statusContents != null && statusContents.length() > 0) extraInformation.setOpenStatus(statusContents);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(restaurantPageURL + " cannot be parsed properly.");
		}
		return extraInformation;
	}

	/**
	 * A helper method to get an image (average rating or picture representing the restaurant).
	 * @param restaurantRatingURL The URL to access the image of interest.
	 * @return An image object containing the relevant information.
     */
	private Bitmap getBusinessImage(String restaurantRatingURL) {
		URL url;
		HttpURLConnection connection = null;
		InputStream input = null;
		Bitmap businessImage = null;
		try {
			url = new URL(restaurantRatingURL);
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.connect();
			input = connection.getInputStream();
			businessImage = BitmapFactory.decodeStream(input);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(connection != null) {
				connection.disconnect();
			}
			if(input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return businessImage;
	}

	/**
	 * returns the sorted List
	 */
	public List<BusinessObjects> getBusinessObjects()
	{
		Collections.sort(mBusinessObjectsList, new RestaurantObjectDistanceComparator()); // sort by descending distance.
		return mBusinessObjectsList;
	}

	/**
	 * @return The number of businesses from the yelp search call.
     */
	public int getTotalBusinesses() {
		return mTotalBusinesses;
	}
}
