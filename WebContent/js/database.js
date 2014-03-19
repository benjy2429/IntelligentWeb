$(window).load(function() {
	var FADESPEED = 250;
	var LOADING_IMG = "<img src='./img/loading.gif' style='vertical-align:text-top;margin-right:5px;' /> Loading..";
	var LOADING_IMG_BIG = "<div style='margin-top:20px;'><img src='./img/big_loading.gif' style='vertical-align:middle;margin-right:10px;' /> Loading..</div>";
	var SERVLET = "DatabaseServlet";
	
	// Prevent forms from performing default action
	$("form").on("submit", function(e) {
		e.preventDefault();
	});
	
	$('#accordion').on('hide.bs.collapse', function (e) {
		$(e.target).parent().removeClass('panel-info').addClass('panel-default');
	});
	$('#accordion').on('show.bs.collapse', function (e) {
		$(e.target).parent().removeClass('panel-default').addClass('panel-info');
	});

	
	$("#showUserFormSubmit").click(function() {
		$("#dynamicText").fadeOut(FADESPEED, function() {
	        $(this).html(LOADING_IMG_BIG).fadeIn(FADESPEED);
	    });
		$.ajax({
			url: SERVLET,
			type: 'post',
			datatype: 'json',
			data: $('#showUserForm').serialize(),
			success: function(data){
				try {
					$("#resultsTitle").text("");
					$("#resultsInfo").text("");
					
					var user = JSON.parse(data);

					var result = "";
					if (!$.isEmptyObject(user)) {	
						result += "<div class='bigUserProfile'>";
						result += "<img class='userProfileImg' src='" + user.profileUrl + "' />";
						result += "<div>";
						result += "<h2>" + user.fullName + "</h2>";
						result += "<h4><b>@" + user.screenName + "</b></h4>";
						result += (user.description) ? "<p>" + user.description + "</p>" : "";
						result += "</div>";
						result += "</div>";
						
						result += "<div class='row'>";
						
						if (user.hometown) {
							result += "<ul class='col-md-3 listGroup'>";
							result += "<li class='list-group-item list-group-item-info'><h4 style='margin:5px 0;'>Hometown</h4></li>";
							result += "<li class='list-group-item'>" + user.hometown + "</h3></li>";
							result += "</ul>";
						}
						
						
							result += "<ul class='col-md-3 listGroup'>";
							result += "<li class='list-group-item list-group-item-info'><h4 style='margin:5px 0;'>Venues visited</h4></li>";
							result += "<li class='list-group-item'>" +  + "</h3></li>";
							result += "</ul>";
							
							
							result += "<ul class='col-md-3 listGroup'>";
							result += "<li class='list-group-item list-group-item-info'><h4 style='margin:5px 0;'>Top keywords</h4></li>";
							result += "<li class='list-group-item'>" +  + "</h3></li>";
							result += "</ul>";

						
							result += "<ul class='col-md-3 listGroup'>";
							result += "<li class='list-group-item list-group-item-info'><h4 style='margin:5px 0;'>Retweeted</h4></li>";
							result += "<li class='list-group-item'>" +  + "</h3></li>";

							result += "<li class='list-group-item list-group-item-info'><h4 style='margin:5px 0;'>Retweeted by</h4></li>";
							result += "<li class='list-group-item'>" +  + "</h3></li>";
							result += "</ul>";

						
						/*
						nearbyVenueResult += "<li class='list-group-item'>";
						nearbyVenueResult += "<h4 class='list-group-item-heading'>" + this.name + "</h4>";
						nearbyVenueResult += "<p class='list-group-item-text'>" + this.vicinity + "</p>";
						nearbyVenueResult += "</li>";
						*/
						result += "</div>";
						result += "</div>";

						
					} else {
						result += "No matching users found.";
					}
	
					$("#dynamicText").fadeOut(FADESPEED, function() {
				        $(this).html(result).fadeIn(FADESPEED);
				    });
				} catch (err) {
					$("#dynamicText").html( "Error: " + err.message ).fadeIn(FADESPEED);
				}
			},
			error: function(xhr,textStatus,errorThrown){
				$("#dynamicText").html(errorThrown);
			}
		});
		
	});
	
	
	$(".results").on('click', '.getRetweets', function(e) {
		var tweet = $(this).parent().parent().parent().parent();
		var tweetId = tweet.find('.tweetId').val();
		$("#retweetsFor" + tweetId).fadeOut(FADESPEED, function() {
	        $(this).html(LOADING_IMG).fadeIn(FADESPEED);
	    });
		$.ajax({
			url: SERVLET,
			type: 'post',
			datatype: 'json',
			data: tweet.find('.retweetersForm').serialize(),
			success: function(data){
				
				var result = " ";
				$.each( JSON.parse(data), function() {
					result += "<a href='#' data-toggle='tooltip' title='" + this.name + "'><img class='tweetImgSmall' src='" + this.profileImageUrl + "'/></a>";
					//result += this.name + " @" + this.screenName + "<br>";
				});				
				if (tweet.find('.retweetCount').val() > 10) {
					result += " + " + (tweet.find('.retweetCount').val()-10) + " more";
				}
				if (!result) {
					$("#retweetsFor" + tweetId).html("No retweets!");
				} else {
				    $("#retweetsFor" + tweetId).fadeOut(FADESPEED, function() {
				        $(this).html(result).fadeIn(FADESPEED);
				        $("[data-toggle='tooltip']").tooltip({ placement: 'bottom' });
				    });
				}
				

			},
			error: function(xhr,textStatus,errorThrown){
				$("Error finding retweeters").insertAfter(tweet);
			}
		});
		
		e.preventDefault();
		
	});
	
	$(".results").on('click', '#expandAllCounts', function(e) {
		var link = $(this);
		var userCounts = link.parent().parent().find('.userCounts');
		userCounts.slideDown(FADESPEED);
		e.preventDefault();
	});
	
	$(".results").on('click', '#collapseAllCounts', function(e) {
		var link = $(this);
		var userCounts = link.parent().parent().find('.userCounts');
		userCounts.slideUp(FADESPEED);
		e.preventDefault();
	});
	
	
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
	
	$(".results").on('click', '#expandAllCheckins', function(e) {
		var link = $(this);
		var venueCheckins = link.parent().find('.venueCheckins');
		venueCheckins.slideDown(FADESPEED);
		e.preventDefault();
	});
	
	$(".results").on('click', '#collapseAllCheckins', function(e) {
		var link = $(this);
		var venueCheckins = link.parent().find('.venueCheckins');
		venueCheckins.slideUp(FADESPEED);
		e.preventDefault();
	});
	
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
	
	$(".results").on('click', '.visitProfile', function(e) {	
		var link = $(this);
		var screenName = link.attr("data-screen-name");
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
					alert("Error fetching profile information for user");
				}
			});
		}
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
					$("#profileTweetsFor" + screenName).html("Error fetching tweets for user profile");
				}
			});
		}
	});
	
	
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
