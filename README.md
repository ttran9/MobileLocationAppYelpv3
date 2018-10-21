# About #

- This was an old project of mine to get familiar with developing on Android and leveraging RESTful API services such as Yelp search v3 to grab information in JSON format and using Java to convert it to objects with the relevant information.
	- In order to grab information from the API call I had to make the API call on a background thread. 
- I also had to extend the BaseAdapter to create a custom view that was able to display an ImageView on each ListItem on the ListView that displays the suggested restaurants from the user's search.

- Looking back at the structure of this project I have a package named controller which actually has the class which implements the API call on the background, this should be moved to the utility package. Since I am no longer working on this project I will not be making this change but if I were to continue working on this project there would be other areas of improvement.