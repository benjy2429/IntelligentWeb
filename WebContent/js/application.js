$(window).load(function() {
	var FADESPEED = 500;
	
	// Prevent forms from performing default action
	$("form").on("submit", function(e) {
		e.preventDefault();
	});
	
	
	$("#form1Submit").click(function() {
		$("#map-canvas").fadeOut(FADESPEED);
		$("#dynamicText").fadeOut(FADESPEED, function() {
	        $(this).html("Loading...").fadeIn(FADESPEED);
	    });
		$.ajax({
			url: 'Servlet',
			type: 'post',
			datatype: 'json',
			data: $('#form1').serialize(),
			success: function(data){
				var json = data.split("\n");
				var tweetIds = JSON.parse(json[0]);
				var tweets = JSON.parse(json[1]);
				$("#resultsTitle").text("Results");
				$("#resultsInfo").text("Below are the most recent results for your query:");
				var result = "";
				if (tweetIds.length > 0 && tweets.length > 0) {
					
					var i = 0;
					$.each( tweets , function() {
						result += "<div class='tweet'>";
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

				$("#dynamicText").fadeOut(FADESPEED, function() {
			        $(this).html(result).fadeIn(FADESPEED);
			    });
			},
			error: function(xhr,textStatus,errorThrown){
				$("#dynamicText").html(errorThrown);
			}
		});
		
	});
	
	
	$("#form2Submit").click(function() {
		$("#map-canvas").fadeOut(FADESPEED);
		$("#dynamicText").fadeOut(FADESPEED, function() {
	        $(this).html("Loading...").fadeIn(FADESPEED);
	    });
		$.ajax({
			url: 'Servlet',
			type: 'post',
			datatype: 'json',
			data: $('#form2').serialize(),
			success: function(data){
				var json = data.split("\n");
				var terms = JSON.parse(json[0]);
				var userObjects = JSON.parse(json[1]);
				$("#resultsTitle").text("Results");
				if(terms.length > 0){
					$("#resultsInfo").text("Below are the " + terms.length + " most frequently used terms:");
				} else {
					$("#resultsInfo").text("There are no frequently used terms from this period!");
				}
				var result = "";
				result += "<div style='border-bottom: 1px solid #e7e7e7;'></div>";
				$.each( terms , function() {
					result += "<div class='frequentWord'>";
					result += "<div class='wordRank'>" + this.rank +".</div>";
					result += "<div class='termStats'>";
					result += "<span class='term'>\"" + this.term  + "\"</span>";
					result += "<span class='termCount'>Number of occurances: " + this.totalCount + "</span>";
					result += "<a href='#' class='viewUserCounts'>Show individual user counts</a>";
					result += "</div>";
					result += "<div class='userCounts'>";
					$.each( this.userCounts , function() { 
						var screenName = this.t;
						var i = 0;
						var index = -1;
						$.each( userObjects , function() {
							if(this.screenName.toLowerCase() == screenName.toLowerCase()){
								index = i;
							}
							i++;
						});
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
				$("#dynamicText").fadeOut(FADESPEED, function() {
			        $(this).html(result).fadeIn(FADESPEED);
			    });
				$.each( userObjects , function() {
					generateModelBox(this);
				});
			},
			error: function(jqXHR,textStatus,errorThrown){
				$("#dynamicText").html(errorThrown);
			}
		});
	});
	
	
	$("#form3Submit").click(function() {
		$("#dynamicText").fadeOut(FADESPEED, function() {
	        $(this).html("Loading...").fadeIn(FADESPEED);
	    });
		$.ajax({
			url: 'Servlet',
			type: 'post',
			datatype: 'json',
			data: $('#form3').serialize(),
			success: function(data){
				var json = data.split("\n");
				var user = JSON.parse(json[0]);
				var venues = JSON.parse(json[1]);
				$("#resultsTitle").text("Results");
				$("#resultsInfo").text("");				
				var result = "";
				// Twitter user info
				
				result += "<div class='tweet' style=''>";
				result += "<img class='tweetImg' src='" + user.profileImageUrl + "' />";
				result += "<div class='tweetContent'>";
				result += "<h2 style='margin:0;'>" + user.name + "'s Latest Foursquare Check-ins</h2>";
				result += "@" + user.screenName;
				result += "</div>";
				result += "</div>";
				
				// Venues
				
				$("#map-canvas").fadeIn(FADESPEED);
				var map = new google.maps.Map(document.getElementById("map-canvas"), {mapTypeId: google.maps.MapTypeId.ROADMAP});
				var bounds = new google.maps.LatLngBounds();

				$.each( venues, function() {
					result += "<div class='venue'>";
					result += (this.photos.groups[1].items.length > 0) ? "<img class='venueImg' src='" + this.photos.groups[1].items[0].url + "'/>" : "";
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
					
			        var marker = new google.maps.Marker({
			            position: new google.maps.LatLng(this.location.lat, this.location.lng),
			            map: map,
			            title: this.name
			        });
			        
			        var infowindow = new google.maps.InfoWindow({
			        	content: "<div class='mapInfobox'><b>" + this.name + "</b><br>"
			        		+ this.location.address + ", " + this.location.city
			        		+ "</div>"
		        	});
			        
			        google.maps.event.addListener(marker, 'click', function() {
			        	infowindow.open(map,marker);
		        	});
			        
			        bounds.extend(marker.position);
			        
				});
				$("#dynamicText").fadeOut(FADESPEED, function() {
			        $(this).html(result).fadeIn(FADESPEED);
			    });
				map.fitBounds(bounds);
			},
			error: function(xhr,textStatus,errorThrown){
				$("#dynamicText").html(errorThrown);
			}
		});
	});
	
	$("#form4Submit").click(function() {
		var parameters = "requestId=" + encodeURIComponent(document.getElementsByName("requestId")[3].value);
		loadXMLDoc(parameters);
	});
	
	$(".results").on('click', '.getRetweets', function(e) {
		var tweet = $(this).parent().parent().parent().parent();
		var tweetId = tweet.find('.tweetId').val();
		$("#retweetsFor" + tweetId).fadeOut(FADESPEED, function() {
	        $(this).html("Loading..").fadeIn(FADESPEED);
	    });
		$.ajax({
			url: 'Servlet',
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
	
	$(".results").on('click', '.viewUserCounts', function(e) {	
		var link = $(this);
		var userCounts = link.parent().parent().find('.userCounts');
		var isVisible = userCounts.is(':visible');
		userCounts.slideToggle(300, function() {
	        if (!isVisible) {
	             link.text("Hide individual user counts");                
	        } else {
	             link.text("Show individual user counts");               
	        }
		});
		e.preventDefault();
	});
	
	$(".results").on('click', '.visitProfile', function(e) {	
		var link = $(this);
		var screenName = link.attr("data-screen-name");
		if (link.attr("data-modal-generated") == "false"){
			$.ajax({
				url: 'Servlet',
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
				url: 'Servlet',
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
		      "<div class='modal-header'>" +
		        "<button type='button' class='close' data-dismiss='modal' aria-hidden='true'>&times;</button>" +
		        "<h4 class='modal-title' id='"+user.screenName+"modalLabel'>Profile for " + user.name + " (@" + user.screenName + ") </h4>" +
		      "</div>" +
		      "<div class='modal-body'>" +
		      	
		      	//Model body
		      	"<div id='profileTweetsFor" + user.screenName + "'>" +
		      		"<p>Loading tweets...</p>" +
		      	"</div>" + 
		      
		      "</div>" +
		      "<div class='modal-footer'>" +
		       "<button type='button' class='btn btn-default' data-dismiss='modal'>Close</button>" +
		        "<button type='button' class='btn btn-primary'>Save changes</button>" +
		      "</div>" +
		    "</div>" +
		  "</div>" +
		"</div>";
		$("#modalWindows").append( result );
		$("a[data-target='#userProfile" + user.screenName + "']").attr('data-modal-generated', 'true');
	}
	
	
	function populateModalTweets(screenName, tweets){
		result = "";
		$.each( tweets, function() {
			result += "<p>" + this.text + "</p>";
		});
		$("#profileTweetsFor" + screenName).html( result );
		$("a[data-target='#userProfile" + screenName + "']").attr('data-tweets-populated', 'true');
	}
	
});
