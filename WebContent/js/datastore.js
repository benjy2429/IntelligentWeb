/*
 * datastore.js - Handles all the javascript for the datastore interface 
 * This includes all AJAX calls to the servlet, and all JSON parsing
 * Author: Ben Carr & Luke Heavens
 * Last Updated: 20/03/2014
 */

$(window).load(function() {
	
	var FADESPEED = 250;
	var LOADING_IMG_BIG = "<div style='margin-top:20px;'><img src='./img/big_loading.gif' style='vertical-align:middle;margin-right:10px;' /> Loading..</div>";
	var SERVLET = "DatastoreServlet";
	
	
	// Prevent forms from performing default action
	$("form").on("submit", function(e) {
		e.preventDefault();
	});
	
	
	// Toggles the active form panel to blue
	$('#accordion').on('hide.bs.collapse', function (e) {
		$(e.target).parent().removeClass('panel-info').addClass('panel-default');
	});
	$('#accordion').on('show.bs.collapse', function (e) {
		$(e.target).parent().removeClass('panel-default').addClass('panel-info');
	});

	
	/*
	 * This function overrides the showUserForm action when the form is submitted
	 * It performs an AJAX call to the servlet to get a matching user
	 */
	$("#showUserFormSubmit").click(function() {	
		// Form validation
		var valid = false;
		var validationError = "Unknown validation error";
		if ( $.trim( $("#username").val() ) != "" ) {
			valid = true;
		} else {
			validationError = "Username cannot be empty";
		}	

		if (valid) {
			$("#pageContent").fadeOut(FADESPEED, function() {
		        $(this).html(LOADING_IMG_BIG).fadeIn(FADESPEED);
		    });
			// Perform the AJAX call
			$.ajax({
				url: SERVLET,
				type: 'post',
				datatype: 'json',
				data: $('#showUserForm').serialize(),
				success: function(data){
					$("#resultsTitle").text("");
					$("#resultsInfo").text("");
					
					// Parse the data objects returned
					json = data.split("\n");
					var user = JSON.parse(json[0]);
					var retweetersOfUser = JSON.parse(json[1]);
					var retweets = JSON.parse(json[2]);
					var locations = JSON.parse(json[3]);
					var keywords = JSON.parse(json[4]);

					var result = "";
					// If data was recieved
					if (!$.isEmptyObject(user)) {
						result += "<div about='twitter:" + user.userId + "'>";
						result += "<span property='bclh:userId' content='" + user.userId + "'/>";
						
						// Write the user information
						result += "<div class='bigUserProfile' style='background-image:url(\"" + user.bannerImgUrl + "\");'>";
						result += "<span property='bclh:bannerImgUrl' content='" + user.bannerImgUrl + "'/>";
						result += "<div class='bigUserProfileDark'>";
						result += "<div class='userProfileImg' style='background-image:url(\"" + user.bigProfileImgUrl + "\");'></div>";
						result += "<span property='bclh:bigProfileImgUrl' content='" + user.bigProfileImgUrl + "'/>";
						result += "<div>";
						result += "<h2 property='schema:name'>" + user.fullName + "</h2>";
						result += "<h4><b>@<span property='schema:alternateName'>" + user.screenName + "</span></b></h4>";
						result += (user.description) ? "<p property='schema:description'>" + user.description + "</p>" : "";
						result += "<p>";
						result += (user.hometown) ? "Hometown: <span property='bclh:hometown'>" + user.hometown + "</span> | " : "";
						result += "<a href='http://twitter.com/" + user.screenName + "'>Visit " + user.fullName + "'s profile on Twitter</a></p>";
						result += "</div>";
						result += "</div>";
						result += "</div>";
						
						result += "<div class='row'>";

						result += "<ul class='col-md-3 listGroup'>";
						result += "<li class='list-group-item list-group-item-info'><h4 style='margin:5px 0;'>Top keywords</h4></li>";
						// If keywords are available, display them, otherwise show an error
						if (keywords.length > 0) {
							var i = 1;
							$.each( keywords, function() {
								result += "<li class='list-group-item clearfix'>";
								result += "<span class='term'>" + i + ". \"" + this.word  + "\"</span>";
								result += " (" + this.count + ")";
								result += "</li>";
								i++;
							});
						} else {
							result += "<li class='list-group-item'>" + user.fullName + " has no keywords</li>";
						}
						result += "</ul>";
						
					
						result += "<ul class='col-md-6 listGroup'>";
						result += "<li class='list-group-item list-group-item-info'><h4 style='margin:5px 0;'>Venues visited</h4></li>";
						// If venues visited are available, display them, otherwise show an error
						if (locations.length > 0) {
							$.each( locations, function() {
								result += "<li class='list-group-item clearfix'>";
								result += "<div class='userProfileVenue'>";
								// Link to the showVenue page for this venue
								result += (this.imageUrl) ? "<a href='' data-venue-name='" + this.name.replace(/'/g, "%27") + "' class='visitVenueProfile userProfileVenueImg' style='background-image:url(\"" + this.imageUrl + "\");' title='See more information about this venue'></a>" : "";
								result += "<div class='venueContent'>";
								result += "<span class='venueName'><a href='' data-venue-name='" + this.name.replace(/'/g, "%27") + "' class='visitVenueProfile' title='See more information about this venue'>" + this.name + "</a>, </span>";
								result += (this.address) ? this.address : "";
								result += (this.address && this.city) ? ", " : "";
								result += (this.city) ? this.city : "";
								result += "<br>";			
								result += (this.websiteUrl) ? "<a href='" + this.websiteUrl + "'>" + this.websiteUrl + "</a><br>" : "";
								result += (this.description) ? this.description : "";
								result += "</div>";
								result += "</div>";
								result += "</li>";
							});
						} else {
							result += "<li class='list-group-item'>" + user.fullName + " has not visited any venues</li>";
						}
						result += "</ul>";
						
						
						result += "<ul class='col-md-3 listGroup' style='max-height:700px;overflow-y:auto;'>";
						result += "<li class='list-group-item list-group-item-info'><h4 style='margin:5px 0;'>Retweeted</h4></li>";
						// If retweets are available, display them, otherwise show an error
						if (retweets.length > 0) {
							$.each( retweets, function() {
								result += "<li class='list-group-item clearfix'>";
								// Link to the showUser page for this user
								result += "<a about='twitter:" + this.userId + "' href='' data-screen-name='" + this.screenName.replace(/'/g, "%27") + "' class='visitUserProfile' title='See more information about this user'>";
								result += "<span property='bclh:userId' content='" + this.userId + "' />";
								result += "<img class='tweetImg' src='" + this.profileImgUrl + "'/>";
								result += "<span property='bclh:profileImgUrl' content='" + this.profileImgUrl + "'/>";
								result += "<div class='tweetContent' style='padding-top:8px;'>";
								result += "<div property='schema:name' class='tweetUser'>" + this.fullName + "</div>";
								result += "<div class='tweetText'>@<span property='schema:alternateName' class='tweetScreenName'>" + this.screenName + "</span></div>";
								result += "</div>";
								result += "</a>";
								result += "</li>";
							});
						} else {
							result += "<li class='list-group-item'>" + user.fullName + " has not retweeted anyone</li>";
						}


						result += "<li class='list-group-item list-group-item-info'><h4 style='margin:5px 0;'>Retweeted by</h4></li>";
						// If retweeters are available, display them, otherwise show an error
						if (retweetersOfUser.length > 0) {
							$.each( retweetersOfUser, function() {
								result += "<li class='list-group-item clearfix'>";
								// Link to the showUser page for this user
								result += "<a about='twitter:" + this.userId + "' href='' data-screen-name='" + this.screenName.replace(/'/g, "%27") + "' class='visitUserProfile' title='See more information about this user'>";
								result += "<span property='bclh:userId' content='" + this.userId + "'/>";
								result += "<img class='tweetImg' src='" + this.profileImgUrl + "'/>";	
								result += "<span property='bclh:profileImgUrl' content='" + this.profileImgUrl + "'/>";
								result += "<div class='tweetContent' style='padding-top:8px;'>";
								result += "<div property='schema:name' class='tweetUser'>" + this.fullName + "</div>";
								result += "<div class='tweetText'>@<span property='schema:alternateName' class='tweetScreenName'>" + this.screenName + "</span></div>";
								result += "</div>";
								result += "</a>";
								result += "</li>";
							});
						} else {
							result += "<li class='list-group-item'>No-one retweeted " + user.fullName + "</li>";
						}
						result += "</ul>";
						result += "</div>";
						result += "</div>";
							
					// If no data was recieved, show an error
					} else {
						result += "No matching users found.";
					}
	
					// Write the data to the page
					$("#pageContent").fadeOut(FADESPEED, function() {
				        $(this).html(result).fadeIn(FADESPEED);
				    });
				},
				error: function(xhr,textStatus,errorThrown){
					$("#pageContent").html("<div class='alert alert-danger'>Error: " + errorThrown + "</div>").fadeIn(FADESPEED);
				}
			});
		} else {
			// Validation error
			$("#pageContent").fadeOut(FADESPEED, function() {
				$("#resultsTitle").text("");
				$("#resultsInfo").text("");
				$("#pageContent").html("<div class='alert alert-danger'>Error: " + validationError + "</div>").fadeIn(FADESPEED);
		    });
		}
	});
	
	
	/*
	 * This function overrides the showUserForm action when the form is submitted
	 * It performs an AJAX call to the servlet to get a matching user
	 */	
	$("#showVenueFormSubmit").click(function() {
		// Form validation
		var valid = false;
		var validationError = "Unknown validation error";
		if ( $.trim( $("#venueName").val() ) != "" ) {
			valid = true;
		} else {
			validationError = "Venue name cannot be empty";
		}	
		
		if (valid) {
			$("#pageContent").fadeOut(FADESPEED, function() {
		        $(this).html(LOADING_IMG_BIG).fadeIn(FADESPEED);
		    });
			$.ajax({
				url: SERVLET,
				type: 'post',
				datatype: 'json',
				data: $('#showVenueForm').serialize(),
				success: function(data){
					$("#resultsTitle").text("");
					$("#resultsInfo").text("");
					
					// Parse the data objects returned
					json = data.split("\n");
					var venue = JSON.parse(json[0]);
					var users = JSON.parse(json[1]);

					var result = "";
					// If data was recieved
					if (!$.isEmptyObject(venue)) {	
						// Write the venue information
						result += "<div class='bigUserProfile'>";
						result += "<div class='bigUserProfileDark'>";
						result += "<div class='userProfileImg' style='background-image:url(\"" + venue.imageUrl + "\");'></div>";
						result += "<div>";
						result += "<h2>" + venue.name + "</h2>";
						result += "<h4><b>";
						result += (venue.address) ? venue.address : "";
						result += (venue.address && venue.city) ? ", " : "";
						result += (venue.city) ? venue.city : "";
						result += "</b></h4>";
						result += (venue.description) ? "<p>" + venue.description + "</p>" : "";
						result += (venue.websiteUrl) ? "<a href='" + venue.websiteUrl + "'>" + venue.websiteUrl + "</a>" : "";
						result += "</div>";
						result += "</div>";
						result += "</div>";
						
						// If user data was recieved, display it, otherwise show an error
						if (users.length > 0) {
							result += "<h3>Users who have visited " + venue.name + "</h3>";
							// Iterate through the users and write their information
							$.each( users, function() {
								// Link to the showUser page for this user
								result += "<a href='' class='visitUserProfile visitUserProfileSmall' data-screen-name='" + this.screenName.replace(/'/g, "%27") + "' data-toggle='tooltip' title='" + this.fullName + "'><img class='venueVisitorImg' src='" + this.profileImgUrl + "'/></a>";
							});
						} else {
							result += "No users have visited " + venue.name;
						}
					} else {
						result += "No matching venues found.";
					}
	
					// Write the data to the page and activate tooltips displaying user names
					$("#pageContent").fadeOut(FADESPEED, function() {
				        $(this).html(result).fadeIn(FADESPEED);
				        $("[data-toggle='tooltip']").tooltip({ placement: 'bottom' });
				    });
				},
				error: function(xhr,textStatus,errorThrown){
					$("#pageContent").html("<div class='alert alert-danger'>Error: " + errorThrown + "</div>").fadeIn(FADESPEED);
				}
			});
		} else {
			// Validation error
			$("#pageContent").fadeOut(FADESPEED, function() {
				$("#resultsTitle").text("");
				$("#resultsInfo").text("");
				$("#pageContent").html("<div class='alert alert-danger'>Error: " + validationError + "</div>").fadeIn(FADESPEED);
		    });
		}		
	});
	
	
	/*
	 * This function fills in the showUser form and displays information for that user
	 * It is called from an onClick event
	 */	
	$(".results").on('click', '.visitUserProfile', function(e) {
		var userScreenName = $(this).data("screen-name").replace("%27", "'");
		$("#username").val(userScreenName);	
		if ($('#userPanel').parent().parent().parent().hasClass("panel-default")) { $('#userPanel').trigger("click"); };		
		$("#showUserFormSubmit").trigger("click");
		e.preventDefault();
	});
	
	
	/*
	 * This function fills in the showVenue form and displays information for that venue
	 * It is called from an onClick event
	 */	
	$(".results").on('click', '.visitVenueProfile', function(e) {
		var userVenueName = $(this).data("venue-name").replace("%27", "'");
		$("#venueName").val(userVenueName);	
		if ($('#venuePanel').parent().parent().parent().hasClass("panel-default")) { $('#venuePanel').trigger("click"); };
		$("#showVenueFormSubmit").trigger("click");
		e.preventDefault();
	});
		
});
