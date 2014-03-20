/*
 * application.js - Handles all the javascript for the web interface 
 * This includes all AJAX calls to the servlet, and all JSON parsing
 * Author: Ben Carr & Luke Heavens
 * Last Updated: 20/03/2014
 */

$(window).load(function() {
	
	var FADESPEED = 250;
	var LOADING_IMG = "<img src='./img/loading.gif' style='vertical-align:text-top;margin-right:5px;' /> Loading..";
	var LOADING_IMG_BIG = "<div style='margin-top:20px;'><img src='./img/big_loading.gif' style='vertical-align:middle;margin-right:10px;' /> Loading..</div>";
	var map = new google.maps.Map(document.getElementById("map-canvas"), {mapTypeId: google.maps.MapTypeId.ROADMAP});
	var bounds = new google.maps.LatLngBounds();
	var streamFunctionId = 0; 
	var SERVLET = "WebServlet";
	
	
	// Prevent forms from performing default action
	$("form").on("submit", function(e) {
		e.preventDefault();
	});
	
	
	// Checkbox to toggle visibility of geolocation fields in tweetForm
	$("#enableLocTweet").click(function() { 
	    if ($(this).is(':checked')) {
	    	$("#locationFieldsTweet").show(FADESPEED);
	    } else {
	    	$("#locationFieldsTweet").hide(FADESPEED);
	    	$("#latTweet").val("");
	    	$("#lonTweet").val("");
	    	$("#radiusTweet").val("");
	    }
	});
	
	
	// Checkbox to toggle visibility of the venue name field in venueForm
	$("#enableNameVenue").click(function() { 
	    if ($(this).is(':checked')) {
	    	$("#nameFieldsVenue").show(FADESPEED);
	    } else {
	    	$("#nameFieldsVenue").hide(FADESPEED);
	    	$("#venueNameVenue").val("");
	    }
	});
	
	
	// Checkbox to toggle visibility of geolocation fields in venueForm
	$("#enableLocVenue").click(function() { 
	    if ($(this).is(':checked')) {
	    	$("#locationFieldsVenue").show(FADESPEED);
	    } else {
	    	$("#locationFieldsVenue").hide(FADESPEED);
	    	$("#latVenue").val("");
	    	$("#lonVenue").val("");
	    	$("#radiusVenue").val("");
	    }
	});
	
	
	// Toggles the active form panel to blue
	$('#accordion').on('hide.bs.collapse', function (e) {
		$(e.target).parent().removeClass('panel-info').addClass('panel-default');
	});
	$('#accordion').on('show.bs.collapse', function (e) {
		$(e.target).parent().removeClass('panel-default').addClass('panel-info');
	});

	
	/*
	 * This function overrides the twitterForm action when the form is submitted
	 * It performs an AJAX call to the servlet to get matching tweets
	 */
	$("#tweetFormSubmit").click(function() {
		// Clear any open streams
		clearInterval(streamFunctionId);
		// Hide Google Maps if visible
		$("#map-canvas").fadeOut(FADESPEED);
		
		// Form validation
		var valid = false;
		var validationError = "Unknown validation error";
		if ( $.trim( $("#queryTweet").val() ) != "" ) {
			if ( $("#enableLocTweet").is(":checked") ) {
				if ( $.trim( $("#latTweet").val() ) != "" &&
					 $.trim( $("#lonTweet").val() ) != "" &&
					 $.trim( $("#radiusTweet").val() ) != "" ) {					
					if ( $.isNumeric( $("#latTweet").val() ) &&
						 $.isNumeric( $("#lonTweet").val() ) &&
						 $.isNumeric( $("#radiusTweet").val() ) ) {
						if ( $("#radiusTweet").val() > 0 ) {
							valid = true;								
						} else {
							validationError = "Radius must be greater than zero";					
						}				
					} else {
						validationError = "Location fields must be numbers";
					}					
				} else {
					validationError = "All location filter fields must be filled in";		
				}				
			} else {
				valid = true;
			}		
		} else {
			validationError = "Search query cannot be empty";
		}	
		
		// Perform an AJAX call
		if (valid) {			
			$("#pageContent").fadeOut(FADESPEED, function() {
		        $(this).html(LOADING_IMG_BIG).fadeIn(FADESPEED);
		    });
			
			$.ajax({
				url: SERVLET,
				type: 'post',
				datatype: 'json',
				data: $('#tweetForm').serialize(),
				success: function(data){
					// Parse the tweetIds and tweet objects separately
					// (Tweet IDs are sent separately due to an int overflow in javascript, causing rounding)
					var json = data.split("\n");
					var tweetIds = JSON.parse(json[0]);
					var tweets = JSON.parse(json[1]);
					$("#resultsTitle").text("Results");
					$("#resultsInfo").text("Below are the most recent results for your query:");
					var result = "";
					
					// Check if data was recieved
					if (tweetIds.length > 0 && tweets.length > 0) {
						var i = 0;
						// For each tweet, add it to the page
						$.each( tweets , function() {
							result += "<div class='tweet'>";
							// Hidden values for getting retweets and showing the profile modal window
							result += "<form class='retweetersForm'><input type='hidden' name='requestId' value='retweetersForm'>";
							result += "<input type='hidden' class='tweetId' name='tweetId' value='" + tweetIds[i] + "'>";
							result += "<input type='hidden' class='retweetCount' name='retweetCount' value='" + this.retweetCount + "'></form>";
							
							result += "<a href='#' data-screen-name='" + this.user.screenName + "' data-modal-generated='false' data-tweets-populated='false' data-toggle='modal' data-target='#userProfile" + this.user.screenName + "' class='visitProfile' title='" + this.user.name + "'>";
							result += "<img class='tweetImg' src='" + this.user.profileImageUrl + "'/>";
							result += "</a>";
							
							result += "<div class='tweetContent'>";							
							result += "<div class='tweetUser'>"; 
							result += "<a href='#' data-screen-name='" + this.user.screenName + "' data-modal-generated='false' data-tweets-populated='false' data-toggle='modal' data-target='#userProfile" + this.user.screenName + "' class='visitProfile' title='" + this.user.name + "'>";
							result += this.user.name + " (@<span class='tweetScreenName'>" + this.user.screenName + "</span>)";
							result += "</a>";
							result +="</div>";
							
							result += "<div class='tweetText'>" + this.text + "</div>";
							result += "<div class='tweetStats'>" + this.createdAt + " ";
							result += "<span class='glyphicon glyphicon-star' title='Favourites' style='margin-left:10px;'></span> " + this.favoriteCount + " ";
							result += "<span class='glyphicon glyphicon-retweet' title='Retweets' style='margin:0 5px 0 10px;'></span> ";
							if ($.isEmptyObject(this.retweetedStatus) && this.retweetCount > 0) {
								result += "<span class='retweets' id='retweetsFor" + tweetIds[i] + "'>" + this.retweetCount + " <a href='#' class='getRetweets'>See who retweeted this</a></span>";
							} else {
								result += "0";
							}
							result += "</div>";
							result += "</div>";
							result += "</div>";
							i++;
						});
						
					} else {
						result += "No matching tweets found. Try searching for a different term, or increasing the location radius.";
					}
	
					// Write the result to the page
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
	 * This function overrides the keywordForm action when the form is submitted
	 * It performs an AJAX call to the servlet to get a list of keywords
	 */
	$("#keywordFormSubmit").click(function() {
		// Clear any open streams
		clearInterval(streamFunctionId);
		// Hide Google Maps if visible
		$("#map-canvas").fadeOut(FADESPEED);
		
		// Form validation
		var valid = false;
		var validationError = "Unknown validation error";
		if ( $.trim( $("#usernamesKeyword").val() ) != "" &&
			 $.trim( $("#keywordsKeyword").val() ) != "" &&
			 $.trim( $("#daysKeyword").val() != "" ) ) {			
			var usernames = $("#usernamesKeyword").val().split(" ");
			if ( usernames.length <= 10 ) {				
				if ( $.isNumeric( $("#keywordsKeyword").val() ) && $("#keywordsKeyword").val() > 0 ) {					
					if ( $.isNumeric( $("#daysKeyword").val() ) && $("#daysKeyword").val() > 0 && $("#daysKeyword").val() < 100 ) {
						valid = true;					
					} else {
						validationError = "Days must be a number between 1 and 99";
					}					
				} else {
					validationError = "Keywords to find must be a number greater than zero";
				}			
			} else {
				validationError = "Up to 10 usernames can be entered only";
			}		
		} else {
			validationError = "All fields must be filled in";
		}	
		
		// Perform an AJAX call
		if (valid) {
			$("#pageContent").fadeOut(FADESPEED, function() {
		        $(this).html(LOADING_IMG_BIG).fadeIn(FADESPEED);
		    });
			
			$.ajax({
				url: SERVLET,
				type: 'post',
				datatype: 'json',
				data: $('#keywordForm').serialize(),
				success: function(data){
					// Parse the terms and user objects separately
					var json = data.split("\n");
					var terms = JSON.parse(json[0]);
					var userObjects = JSON.parse(json[1]);
					$("#resultsTitle").text("Results");
					
					// Check if data was recieved
					if(terms.length > 0){
						var html = "Below are the " + terms.length + " most frequently used terms:";
						html += "<br/><a href='#' id='expandAllCounts'>Expand All</a> | <a href='#' id='collapseAllCounts'>Collapse All</a>";
						$("#resultsInfo").html(html);
					} else {
						$("#resultsInfo").text("There are no frequently used terms from this period!");
					}
					var result = "";
					// Iterate through the terms
					$.each( terms , function() {
						result += "<div class='frequentWord'>";
						result += "<div class='wordRank'>" + this.rank +".</div>";
						result += "<div class='termStats'>";
						result += "<span class='term'>\"" + this.term  + "\"</span>";
						result += "<span class='termCount'>Number of occurances: " + this.totalCount + "</span>";
						result += "<a href='#' class='viewUserCounts'>Show individual user counts</a>";
						result += "</div>";
						result += "<div class='userCounts'>";
						
						// Find users who have a count for this term
						$.each( this.userCounts , function() { 
							var screenName = this.t;
							var i = 0;
							var index = -1;
							$.each( userObjects , function() {
								if(this.screenName.toString().toLowerCase() == screenName.toString().toLowerCase()){
									index = i;
								}
								i++;
							});
							// If a user has a count, display their information
							if (index > -1){
								var user = userObjects[index];
								result += "<div class='userCount'>";
								result += "<a href='#' data-screen-name='" + user.screenName + "' data-modal-generated='false' data-tweets-populated='false' data-toggle='modal' data-target='#userProfile" + user.screenName + "' class='visitProfile' title='" + user.name + "'>";
								result += "<img class='tweetImgSmall' src='" + user.profileImageUrl + "'/>" + user.name + " (@<span class='tweetScreenName'>" + user.screenName + "</span>)";
								result += "</a> : " + this.u;
								result += "</div>";
							}
						});
						result += "</div>";
						result += "</div>";
					});
					
					// Write the result to the page
					$("#pageContent").fadeOut(FADESPEED, function() {
				        $(this).html(result).fadeIn(FADESPEED);
				    });
					
					// Generate profile modal windows for the users
					$.each( userObjects , function() {
						generateModelBox(this);
					});
				},
				error: function(jqXHR,textStatus,errorThrown){
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
	 * This function overrides the checkinForm action when the form is submitted
	 * It performs an AJAX call to the servlet to get a list of check-ins, and also handles repeat calls for live streaming
	 */
	$("#checkinFormSubmit").click(function() {
		// Clear any open streams
		clearInterval(streamFunctionId);
		// Hide Google Maps if visible
		$("#map-canvas").fadeOut(FADESPEED);
		
		// Form validation
		var valid = false;
		var validationError = "Unknown validation error";
		if ( $.trim( $("#usernameCheckin").val() ) != "" ) {
			if ( $.trim( $("#daysCheckin").val() ) != "" ) {
				if ( $.isNumeric( $("#daysCheckin").val() ) && $("#daysCheckin").val() >= 0 ) {
					valid = true;
				} else {
					validationError = "Days to search must be a number greater than or equal to 0";
				}				
			} else {
				validationError = "Days field cannot be empty";
			}		
		} else {
			validationError = "Username field cannot be empty";
		}			
		
		if (valid) {
			// Create a new Google Maps object
			map = new google.maps.Map(document.getElementById("map-canvas"), {mapTypeId: google.maps.MapTypeId.ROADMAP});
			bounds = new google.maps.LatLngBounds();
		
			// Loader
			$("#pageContent").fadeOut(FADESPEED, function() {
		        $(this).html(LOADING_IMG_BIG).fadeIn(FADESPEED);
		    });
			
			// Perform an AJAX call
			getUserVenues(true);
			
			// If live stream required
			if ($("#daysCheckin").val() == "0") {
				$("#userRequest").val("0");
				// Set the stream function and call every 20 seconds
				streamFunctionId = setInterval(function() { getUserVenues(false); }, 20000);
			} else {
				// Clear the stream
				clearInterval(streamFunctionId);
			}
			
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
	 * This function performs an AJAX call to the servlet to get a list of check-ins
	 */
	function getUserVenues(userRequest) {
		$.ajax({
			url: SERVLET,
			type: 'post',
			datatype: 'json',
			data: $('#checkinForm').serialize(),
			success: function(data){
				var result = "";
				var venues;
				($("#daysVenue").val() == "0") ? $("#resultsTitle").text("Results - Live Stream (Refreshes every 20 seconds)") : $("#resultsTitle").text("Results");
				$("#resultsInfo").text("");				
			
				// User request is only required on the first AJAX call
				if (userRequest) {
					// Parse the user object and display their information
					var json = data.split("\n");
					var user = JSON.parse(json[0]);
					venues = JSON.parse(json[1]);
					// Twitter user info	
					result += "<div class='tweet' style=''>";
					result += "<a href='#' data-screen-name='" + user.screenName + "' data-modal-generated='false' data-tweets-populated='false' data-toggle='modal' data-target='#userProfile" + user.screenName + "' class='visitProfile' title='" + user.name + "'>";
					result += "<img class='tweetImg' src='" + user.profileImageUrl + "' />";
					result += "</a>";
					result += "<div class='tweetContent'>";
					result += "<h2 style='margin:0;'>" + user.name + "'s Latest Foursquare Check-ins</h2>";
					result += "<a href='#' data-screen-name='" + user.screenName + "' data-modal-generated='false' data-tweets-populated='false' data-toggle='modal' data-target='#userProfile" + user.screenName + "' class='visitProfile' title='" + user.name + "'>";
					result += "@" + user.screenName;
					result += "</a>";
					result += "</div>";
					result += "</div>";			
				} else {
					// Only parse the venues
					venues = JSON.parse(data);
				}
				
				$("#map-canvas").fadeIn(FADESPEED);
				
				// Iterate through the venues and display their information
				$.each( venues, function() {
					result += "<div class='venue'>";
					result += (this.photos.groups[1] && this.photos.groups[1].items.length > 0) ? "<div class='venueImg' style='background-image:url(\"" + this.photos.groups[1].items[0].url + "\");'/>" : "";
					result += "<div class='venueContent'>";
					result += "<span class='venueName'>" + this.name + ", </span>";
					result += (this.location.address) ? this.location.address : "";
					result += (this.location.address && this.location.city) ? ", " : "";
					result += (this.location.city) ? this.location.city : "";
					result += "<br>";			
					var categories = [];
					$.each( this.categories, function() {
						categories.push( "<img src='" + this.icon + "' height='15' style='vertical-align:text-top' /> " + this.name );
					});
					result += categories.join(", ") + "<br>";
					result += (this.url) ? "<a href='" + this.url + "'>" + this.url + "</a><br>" : "";
					result += (this.description) ? this.description : "";
					result += "</div>";
					result += "</div>";
					
					// Add a marker to the Google Map at the venue's location
			        var marker = new google.maps.Marker({
			            position: new google.maps.LatLng(this.location.lat, this.location.lng),
			            map: map,
			            title: this.name
			        });
			        
			        // Add an infoWindow at the marker to display extra information on the map 
			        var infowindow = new google.maps.InfoWindow({
			        	content: "<div class='mapInfobox'><b>" + this.name + "</b><br>"
			        		+ this.location.address + ", " + this.location.city
			        		+ "</div>"
		        	});
			        
			        // Add the infoWindow to a click event listener
			        google.maps.event.addListener(marker, 'click', function() {
			        	infowindow.open(map,marker);
		        	});
			        
			        // Extend the map bounds so all markers are in the initial view
			        bounds.extend(marker.position); 
				});
				
				// Write information to the page or add new venues as they are streamed
				if (!userRequest) {
					$(".tweet").after(result).fadeIn(FADESPEED);
				} else {
					$("#pageContent").fadeOut(FADESPEED, function() {
				        $(this).html(result).fadeIn(FADESPEED);
				    });
				}
				
				// Refresh the Google Map to show all markers and set the maximum zoom level
				google.maps.event.trigger(map, 'resize');
				map.fitBounds(bounds);
				if (map.getZoom() > 15) map.setZoom(15);

			},
			error: function(xhr,textStatus,errorThrown){
				clearInterval(streamFunctionId);
				$("#pageContent").html("<div class='alert alert-danger'>Error: " + errorThrown + "</div>").fadeIn(FADESPEED);
			}
		});
	}
	
	
	/*
	 * This function overrides the venueForm action when the form is submitted
	 * It performs an AJAX call to the servlet to get a list of venues, and also handles repeat calls for live streaming
	 */
	$("#venueFormSubmit").click(function() {
		// Clear any open streams
		clearInterval(streamFunctionId);
		// Hide Google Maps if visible
		$("#map-canvas").fadeOut(FADESPEED);
		
		// Form validation
		var valid = false;
		var nameValid = false;
		var locValid = false;
		var validationError = "Unknown validation error";
		if ( $.trim( $("#daysVenue").val() ) != "" ) {
			if ( $.isNumeric( $("#daysVenue").val() ) && $("#daysVenue").val() >= 0 ) {
				if ( $("#enableNameVenue").is(":checked") ) {
					if ( $.trim( $("#venueNameVenue").val() ) != "" ) {
						nameValid = true;
					} else {
						validationError = "Venue name field cannot be empty";
					}
				}								
				if ( $("#enableLocVenue").is(":checked") ) {
					if ( $.trim( $("#latVenue").val() ) != "" &&
						 $.trim( $("#lonVenue").val() ) != "" &&
						 $.trim( $("#radiusVenue").val() ) != "" ) {					
						if ( $.isNumeric( $("#latVenue").val() ) &&
								 $.isNumeric( $("#lonVenue").val() ) &&
								 $.isNumeric( $("#radiusVenue").val() ) ) {
								if ( $("#radiusVenue").val() > 0 ) {
									locValid = true;										
								} else {
									validationError = "Radius must be greater than zero";					
								}						
							} else {
								validationError = "Location fields must be numbers";
							}
					} else {
						validationError = "Location fields must not be empty";
					}
				}				
				if ( ($("#enableNameVenue").is(":checked") && !$("#enableLocVenue").is(":checked") && nameValid) ||
					 ($("#enableLocVenue").is(":checked") && !$("#enableNameVenue").is(":checked") && locValid) ||
					 ($("#enableNameVenue").is(":checked") && nameValid && $("#enableLocVenue").is(":checked") && locValid) ) {
					valid = true;
				} else if ( !$("#enableNameVenue").is(":checked") && !$("#enableLocVenue").is(":checked") ) {
					validationError = "You must search by venue name or location or both";
				}
			} else {
				validationError = "Days to search must be a number greater than or equal to 0";
			}		
		} else {
			validationError = "Days to search cannot be empty";
		}
					
		if (valid) {
			// Create a new Google Maps object
			map = new google.maps.Map(document.getElementById("map-canvas"), {mapTypeId: google.maps.MapTypeId.ROADMAP});
			bounds = new google.maps.LatLngBounds();
			
			// Loader
			$("#pageContent").fadeOut(FADESPEED, function() {
		        $(this).html(LOADING_IMG_BIG).fadeIn(FADESPEED);
		    });
			
			// Perform an AJAX call
			getUsersAtVenue();
			
			// If live stream required
			if ($("#daysVenue").val() == "0") {
				// Set the stream function and call every 20 seconds
				streamFunctionId = setInterval(function() { getUsersAtVenue(); }, 20000);
			} else {
				// Clear the stream
				clearInterval(streamFunctionId);
			}
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
	 * This function performs an AJAX call to the servlet to get a list of venues
	 */
	function getUsersAtVenue() { //TODO Combine same venue when streaming
		$.ajax({
			url: SERVLET,
			type: 'post',
			datatype: 'json',
			data: $('#venueForm').serialize(),
			success: function(data){								
				($("#daysVenue").val() == "0") ? $("#resultsTitle").text("Results - Live Stream (Refreshes every 20 seconds)") : $("#resultsTitle").text("Results");
				$("#resultsInfo").text("");		
				// Parse the venues and tweets separately
				var json = data.split("\n");
				var venues = JSON.parse(json[0]);
				var venueTweetsMap = JSON.parse(json[1]);
				var result = "";
				var i = 0;

				// Check if data was recieved
				if ( !$.isEmptyObject(venues) ) {
					if ( $("#pageContent").find(".venue").length == 0 ) {
						result += ($("#enableLocVenue").is(":checked")) ? "<div class='row'><div class='col-md-8'>" : "";
						result += "<h2 style='margin:20px 0;'>Check-ins in this area</h2>";
						result += "<a href='#' id='expandAllCheckins'>Expand all venue check-ins</a> | ";
						result += "<a href='#' id='collapseAllCheckins'>Collapse all venue check-ins</a>";
					}
					// Iterate through the venues and write their information
					$.each(venues, function(venueId,venueObj){	
						result += "<div class='venueSection'>";
						result += "<div class='venue venueDark'>";
						result += (venueObj.photos.groups[1] && venueObj.photos.groups[1].items.length > 0) ? "<div class='venueImg' style='background-image:url(\"" + venueObj.photos.groups[1].items[0].url + "\");'/>" : "";
						result += "<div class='venueContent'>";
						result += "<span class='venueName'>" + venueObj.name + ", </span>";
						result += (venueObj.location.address) ? venueObj.location.address : "";
						result += (venueObj.location.address && venueObj.location.city) ? ", " : "";
						result += (venueObj.location.city) ? venueObj.location.city : "";
						result += "<br>";			
						var categories = [];
						$.each( venueObj.categories, function() {
							categories.push( "<img src='" + this.icon + "' height='15' style='vertical-align:text-top' /> " + this.name );
						});
						result += categories.join(", ") + "<br>";
						result += (venueObj.url) ? "<a href='" + venueObj.url + "'>" + venueObj.url + "</a><br>" : "";
						result += (venueObj.description) ? venueObj.description : "";
						result += "</div>";
						result += "<div class='showVenueCheckinsContainer'>";
						result += "<a href='#' class='showVenueCheckins'>Show check-ins</a>";
						result += "</div>";
						result += "</div>";
	
						// Iterate through the tweets and display them under the venue
					    result += "<div class='venueCheckins' style='display:none'>";
					    $.each(venueTweetsMap[venueId], function(id,tweetObj){
							result += "<div class='tweet checkin'>";							
							result += "<a href='#' data-screen-name='" + tweetObj.user.screenName + "' data-modal-generated='false' data-tweets-populated='false' data-toggle='modal' data-target='#userProfile" + tweetObj.user.screenName + "' class='visitProfile' title='" + tweetObj.user.name + "'>";
							result += "<img class='tweetImg' src='" + tweetObj.user.profileImageUrl + "'/>";
							result += "</a>";							
							result += "<div class='tweetContent'>";							
							result += "<div class='tweetUser'>"; 
							result += "<a href='#' data-screen-name='" + tweetObj.user.screenName + "' data-modal-generated='false' data-tweets-populated='false' data-toggle='modal' data-target='#userProfile" + tweetObj.user.screenName + "' class='visitProfile' title='" + tweetObj.user.name + "'>";
							result += tweetObj.user.name + " (@<span class='tweetScreenName'>" + tweetObj.user.screenName + "</span>)";
							result += "</a>";
							result +="</div>";						
							result += "<div class='tweetText'>" + tweetObj.text + "</div>";
							result += "<div class='tweetStats'>" + tweetObj.createdAt + "</div>";
							result += "</div>";
							result += "</div>";
					    });	
					    result += "</div>";
					    				    
					    // Add a marker to the Google Map at the venue's location
				        var marker = new google.maps.Marker({
				            position: new google.maps.LatLng(venueObj.location.lat, venueObj.location.lng),
				            map: map,
				            title: venueObj.name
				        });
				        
				        // Add an infoWindow at the marker to display extra information on the map 
				        var infowindow = new google.maps.InfoWindow({
				        	content: "<div class='mapInfobox'><b>" + venueObj.name + "</b><br>"
				        		+ venueObj.location.address + ", " + venueObj.location.city
				        		+ "</div>"
			        	});
				        
				        // Add the infoWindow to a click event listener
				        google.maps.event.addListener(marker, 'click', function() {
				        	infowindow.open(map,marker);
			        	});
				        
				        // Extend the map bounds so all markers are in the initial view
				        bounds.extend(marker.position);
				        
					    i++;
					    result += "</div>";

					});	
					
					// Display popular venues if this is the first call and if a location was specified
					if ( $("#pageContent").find(".venue").length == 0 && $("#enableLocVenue").is(":checked") ) {
						result += "</div>";
						result += "<div class='col-md-4'>";
						result += "<ul class='listGroup nearbyVenues'>";
						result += "<li class='list-group-item list-group-item-info'><h4 style='margin:5px 0;'>Nearby Popular Venues</h4></li>";
						result += "</ul>";
						result += "</div></div>";
						
						getNearbyVenues($("#latVenue").val(), $("#lonVenue").val(), $("#radiusVenue").val());
					}
					
					// Show the map if not currently visible
				    if (!$("#map-canvas").is(":visible")) $("#map-canvas").fadeIn(FADESPEED);
				    
				    // Refresh the Google Map to show all markers and set the maximum zoom level
					google.maps.event.trigger(map, 'resize');
					map.fitBounds(bounds);
					if (map.getZoom() > 15) map.setZoom(15);
					
				} else {
					// If no data was recieved, show an error and hide the Google Map
					if ($("#daysVenue").val() != "0") {
						result = "No users have visited this location. Try broadening the search by increasing the location radius or the number of days to search .";
						if ($("#map-canvas").is(":visible")) $("#map-canvas").fadeOut(FADESPEED);	
					}
				}
				
				// Write information to the page or add new venues as they are streamed
				if ( $("#pageContent").find(".venue").length > 0 ) {
					$(".venueSection").prepend(result).fadeIn(FADESPEED);
				} else {
					$("#pageContent").fadeOut(FADESPEED, function() {
				        $(this).html(result).fadeIn(FADESPEED);
				    });

				}
			},
			error: function(xhr,textStatus,errorThrown){
				$("#pageContent").html("<div class='alert alert-danger'>Error: " + errorThrown + "</div>").fadeIn(FADESPEED);
			}
		});		
	}
	

	/*
	 * This function performs an AJAX call to the servlet to get a list of venues near a geolocation
	 */
	function getNearbyVenues(lat, lon, radius) {
		var nearbyVenueResult = "";
		$.ajax({
			url: SERVLET,
			type: 'post',
			datatype: 'json',
			data: { "requestId": "getNearbyVenues", "lat": lat, "lon": lon, "radius": radius },
			success: function(data){
				// Parse the JSON object of venues
				var venues = JSON.parse(data);
				
				// Check if data was recieved
				if ( venues.length > 0 ) {
					// Iterate through the venues and display their information
					$.each(venues, function(){	
						nearbyVenueResult += "<li class='list-group-item'>";
						nearbyVenueResult += "<h4 class='list-group-item-heading'>" + this.name + "</h4>";
						nearbyVenueResult += "<p class='list-group-item-text'>" + this.vicinity + "</p>";
						nearbyVenueResult += "</li>";
							
						// Create a new custom marker
						var image = {
						    url: this.icon,
						    scaledSize: new google.maps.Size(20, 20),
						    origin: new google.maps.Point(0,0),
						    anchor: new google.maps.Point(10, 20)
						};

						// Add a marker to the Google Map at the venue's location
				        var marker = new google.maps.Marker({
				            position: new google.maps.LatLng(this.geometry.location.lat, this.geometry.location.lng),
				            map: map,
				            title: this.name,
				            icon: image
				        });
				        
				        // Add an infoWindow at the marker to display extra information on the map 
				        var infowindow = new google.maps.InfoWindow({
				        	content: "<div class='mapInfobox'><b>" + this.name + "</b><br>"
				        		+ this.vicinity + "</div>"
			        	});
				        
				        // Add the infoWindow to a click event listener
				        google.maps.event.addListener(marker, 'click', function() {
				        	infowindow.open(map,marker);
			        	});
				        
				        // Extend the map bounds so all markers are in the initial view
				        bounds.extend(marker.position);				
					});	
					
					nearbyVenueResult += "<li class='list-group-item'><span class='text-muted'>Popular venue data from Google Places</span></li>";
					// Append the venue HTML to the nearbyVenues div
					$(".nearbyVenues").append(nearbyVenueResult);
					
				} else {
					// If no data was recieved, display an error
					nearbyVenueResult += "<li class='list-group-item'>No venues nearby</li>";
				}
			},
			error: function(xhr,textStatus,errorThrown){
				$("#pageContent").html("<div class='alert alert-danger'>Error: " + errorThrown + "</div>").fadeIn(FADESPEED);
			}
		});		
	}
	
	
	/*
	 * This function performs an AJAX call to the servlet to get a list of users who have retweeted a given tweet
	 * It is called from an onClick event
	 */
	$(".results").on('click', '.getRetweets', function(e) {
		// Get the tweetId from the HTML
		var tweet = $(this).parent().parent().parent().parent();
		var tweetId = tweet.find('.tweetId').val();
		
		$("#retweetsFor" + tweetId).fadeOut(FADESPEED, function() {
	        $(this).html(LOADING_IMG).fadeIn(FADESPEED);
	    });
		
		// Perform the AJAX call
		$.ajax({
			url: SERVLET,
			type: 'post',
			datatype: 'json',
			data: tweet.find('.retweetersForm').serialize(),
			success: function(data){
				var result = " ";
				// Parse the users and write their profile pictures to the page
				$.each( JSON.parse(data), function() {
					result += "<a href='#' data-toggle='tooltip' title='" + this.name + "'><img class='tweetImgSmall' src='" + this.profileImageUrl + "'/></a>";
					//result += this.name + " @" + this.screenName + "<br>";
				});				
				// If there are more than 10 retweets, add a "+XX more" message
				if (tweet.find('.retweetCount').val() > 10) {
					result += " + " + (tweet.find('.retweetCount').val()-10) + " more";
				}
				// If no data was recieved, display an error
				if (!result) {
					$("#retweetsFor" + tweetId).html("No retweets!");
				} else {
				// Write the result to the page and add a tooltip displaying their name
				    $("#retweetsFor" + tweetId).fadeOut(FADESPEED, function() {
				        $(this).html(result).fadeIn(FADESPEED);
				        $("[data-toggle='tooltip']").tooltip({ placement: 'bottom' });
				    });
				}
			},
			error: function(xhr,textStatus,errorThrown){
				$("#pageContent").prepend("<div class='alert alert-danger'>Error: " + errorThrown + "</div>").fadeIn(FADESPEED);
			}
		});	
		// Prevent the anchor tag from going to its href location
		e.preventDefault();		
	});

	
	/*
	 * This function expands all keyword divs to show all individual user counts
	 * It is called from an onClick event
	 */
	$(".results").on('click', '#expandAllCounts', function(e) {
		var link = $(this);
		var userCounts = link.parent().parent().find('.userCounts');
		userCounts.slideDown(FADESPEED);
		e.preventDefault();
	});
	
	
	/*
	 * This function collapses all keyword divs to hide all individual user counts
	 * It is called from an onClick event
	 */
	$(".results").on('click', '#collapseAllCounts', function(e) {
		var link = $(this);
		var userCounts = link.parent().parent().find('.userCounts');
		userCounts.slideUp(FADESPEED);
		e.preventDefault();
	});
	
	
	/*
	 * This function slides the userCounts div up and down when it is displayed or hidden
	 * It is called from an onClick event
	 */	
	$(".results").on('click', '.viewUserCounts', function(e) {	
		var link = $(this);
		var userCounts = link.parent().parent().find('.userCounts');
		var isVisible = userCounts.is(':visible');
		userCounts.slideToggle(FADESPEED, function() {
	        if (!isVisible) {
	             link.text("Hide individual user counts");                
	        } else {
	             link.text("Show individual user counts");               
	        }
		});
		e.preventDefault();
	});
	
	
	/*
	 * This function expands all checkin divs to show all individual check-in tweets
	 * It is called from an onClick event
	 */
	$(".results").on('click', '#expandAllCheckins', function(e) {
		var link = $(this);
		var venueCheckins = link.parent().find('.venueCheckins');
		venueCheckins.slideDown(FADESPEED);
		e.preventDefault();
	});
	
	
	/*
	 * This function collapses all checkin divs to hide all individual check-in tweets
	 * It is called from an onClick event
	 */
	$(".results").on('click', '#collapseAllCheckins', function(e) {
		var link = $(this);
		var venueCheckins = link.parent().find('.venueCheckins');
		venueCheckins.slideUp(FADESPEED);
		e.preventDefault();
	});
	
	
	/*
	 * This function slides the venueCheckins div up and down when it is displayed or hidden
	 * It is called from an onClick event
	 */	
	$(".results").on('click', '.showVenueCheckins', function(e) {	
		var link = $(this);
		var venueCheckins = link.parent().parent().parent().find('.venueCheckins');
		var isVisible = venueCheckins.is(':visible');
		venueCheckins.slideToggle(FADESPEED, function() {
	        if (!isVisible) {
	             link.text("Hide check-ins");                
	        } else {
	             link.text("Show check-ins");               
	        }
		});
		e.preventDefault();
	});
	
	
	/*
	 * This function performs an AJAX to get a user object and their recent tweets for display in a profile modal window
	 * It is called from an onClick event
	 */	
	$(".results").on('click', '.visitProfile', function(e) {	
		var link = $(this);
		var screenName = link.attr("data-screen-name");
		// Perform the AJAX call if the user data is not already available
		if (link.attr("data-modal-generated") == "false"){
			$.ajax({
				url: SERVLET,
				type: 'post',
				datatype: 'json',
				data: "requestId=fetchUserForProfile&screenName="+ screenName + "",
				success: function(data){
					generateModelBox(JSON.parse(data));
					$("#userProfile" + screenName).modal("show"); //If generating modal then the click event to show it might not be executed if model hasnt yet been defined, hence we ensure it is shown after construction
				},
				error: function(xhr,textStatus,errorThrown){
					$("#pageContent").prepend("<div class='alert alert-danger'>Error: " + errorThrown + "</div>").fadeIn(FADESPEED);
				}
			});
		}
		// Perform the AJAX call if the recent tweets are not already available
		if (link.attr("data-tweets-populated") == "false"){
			$.ajax({
				url: SERVLET,
				type: 'post',
				datatype: 'json',
				data: "requestId=fetchTweetsForProfile&screenName="+ screenName,
				success: function(data){
					populateModalTweets(screenName, JSON.parse(data));
				},
				error: function(xhr,textStatus,errorThrown){
					$("#profileTweetsFor" + screenName).html("<div class='alert alert-danger'>Error: " + errorThrown + "</div>").fadeIn(FADESPEED);
				}
			});
		}
	});
	
	
	/*
	 * This function writes the HTML for a modal window used to display a users profile
	 * It takes a user object as a parameter
	 */	
	function generateModelBox(user){
		result = "" +
		"<div class='modal fade' id='userProfile" + user.screenName + "' tabindex='-1' role='dialog' aria-labelledby='"+user.screenName+"modalLabel' aria-hidden='true'>" +
		  "<div class='modal-dialog'>" +
		    "<div class='modal-content'>" +
		      "<div class='modal-header'";
		      result += (user.profileBannerImageUrl) ? " style='background-image:url(\"" + user.profileBannerImageUrl + "/web\")'>" : ">";
      		  	result += "<div class='userProfileBannerDark'>" +
	  			  "<button type='button' class='close' data-dismiss='modal' aria-hidden='true'>&times;</button>" +
					"<div class='media'>" +
					"<div class='pull-left'>" +
						"<img class='media-object tweetImg' src='" + user.profileImageUrl + "' alt='...'>" +
					"</div>" +
					"<div class='media-body'>" +
						"<h4 class='media-heading'><b>" + user.name + "</b> (@" + user.screenName + ")</h4>";
						result += (user.description) ? "<p>" + user.description + "</p>" : "";
					result += "</div>" +
				  "</div>" +
				"</div>" +
		      "</div>" +
		      "<div class='row userProfileStats'>" +		      
		      	"<div class='col-md-2 col-sm-2'>Tweets<br>" + user.statusesCount + "</div>" +
		      	"<div class='col-md-2 col-sm-2'>Following<br>" + user.friendsCount + "</div>" +
		      	"<div class='col-md-2 col-sm-2'>Followers<br>" + user.followersCount + "</div>";
		      	result += (user.location) ? "<div class='col-md-3 col-sm-3'>Hometown<br>" + user.location + "</div>" : "";
		      result += "</div>" +
		      "<div class='modal-body'>" +		      
		      	//Model body
		      	"<div id='profileTweetsFor" + user.screenName + "' class='profileTweets'>" +
		      		LOADING_IMG_BIG +
		      	"</div>" + 
		      	
		      	"<a href='http://twitter.com/" + user.screenName + "/' class='profileLink'>Visit " + user.name + "'s profile on Twitter</a>" +
		      
		      "</div>" +
		    "</div>" +
		  "</div>" +
		"</div>";
		$("#modalWindows").append( result );
		$("a[data-target='#userProfile" + user.screenName + "']").attr('data-modal-generated', 'true');
	}
	
	
	/*
	 * This function writes the latest tweets to a profile modal window
	 * It takes a screen name and a list of tweets as parameters
	 */	
	function populateModalTweets(screenName, tweets){
		lastLocationFound = false;
		result = "";
		result += "<h3>Latest Tweets</h3>";
		$.each( tweets, function() {
			result += "<div class='modalTweet'>" + this.text + "</div>";
			if (!lastLocationFound && this.place) {
				$("#userProfile" + screenName).find(".userProfileStats").append("<div class='col-md-3 col-sm-3'>Last Location<br><span class='profileLastLocation'>" + this.place.name + "</span></div>");
				lastLocationFound = true;
			}
		});
		$("#profileTweetsFor" + screenName).html( result );
		$("a[data-target='#userProfile" + screenName + "']").attr('data-tweets-populated', 'true');
	}
	
});
