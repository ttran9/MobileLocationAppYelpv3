package restaurantfinder.example.tran.yelpfindrestaurants.model;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Class that allows the user to make a search using the yelp API
 */
public class Yelp {
    /**
     * A string generated from a POST request to https://api.yelp.com/oauth2/token, with proper credentials
     */
    private String mAccessToken;

    /**
    * Set up the access token to authenticate API calls with search v3
    */
    public Yelp(String accessToken)
    {
        this.mAccessToken = accessToken;
    }

    /**
     * Search with term and location.
     *
     * @param term Search term.
     * @param latitude Latitude.
     * @param longitude Longitude.
     * @param limitSearchValue number of businesses.
     * @param sortValue sort type.
     * @param offsetValue the range of businesses to return.
     * @return JSON string response.
    */
    public String search(String term, double latitude, double longitude, Integer limitSearchValue, String sortValue, int offsetValue) {
        String urlParameters = "term=" + term + "&latitude=" + String.valueOf(latitude) + "&longitude=" + String.valueOf(longitude) + "&limit=" + String.valueOf(limitSearchValue) + "&sort_by=" + sortValue + "&open_now_filter=true" + "&offset=" + offsetValue;
        String searchURL = "https://api.yelp.com/v3/businesses/search?" + urlParameters;
        StringBuffer response = new StringBuffer();
        try {
            URL myURL = new URL(searchURL);
            HttpURLConnection myURLConnection = (HttpURLConnection)myURL.openConnection();
            String basicAuth = "Bearer " + mAccessToken;

            myURLConnection.setRequestProperty("Authorization", basicAuth);

            BufferedReader in = new BufferedReader(new InputStreamReader(myURLConnection.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        } catch(ProtocolException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }
        return response.toString();

    }
}
