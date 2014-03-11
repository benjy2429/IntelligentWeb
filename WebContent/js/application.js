$(window).load(function() {

	
	// Prevent forms from performing default action
	$("form").on("submit", function(e) {
		e.preventDefault();
	});
	
	
	$("#form1Submit").click(function() {
		$("#dynamicText").html("Loading...");
		$.ajax({
			url: 'Servlet',
			type: 'post',
			datatype: 'json',
			data: $('#form1').serialize(),
			success: function(data){
				var json = data.split("\n");
				var tweetIds = JSON.parse(json[0]);
				var tweets = JSON.parse(json[1]);

				var result = "";
				
				if (tweetIds.length > 0 && tweets.length > 0) {
					
					var i = 0;
					$.each( tweets , function() {
						result += "<div class='tweet'>";
						result += "<form class='retweetersForm'><input type='hidden' name='formId' value='retweetersForm'>";
						result += "<input type='hidden' class='tweetId' name='tweetId' value='" + tweetIds[i] + "'>";
						result += "<input type='hidden' class='retweetCount' name='retweetCount' value='" + this.retweetCount + "'></form>";
						result += "<img class='tweetImg' src='" + this.user.profileImageUrl + "'/>";
						result += "<div class='tweetContent'>";
						result += "<div class='tweetUser'>" + this.user.name + " (@<span class='tweetScreenName'>" + this.user.screenName + "</span>)</div>";
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
					result += "No matching tweets found. Try searching for a different term, or increasing the location radius."
				}

				$("#dynamicText").html(result);

			},
			error: function(xhr,textStatus,errorThrown){
				$("#dynamicText").html(errorThrown);
			}
		});
		
	});
	
	
	$("#form2Submit").click(function() {
		$("#dynamicText").html("Loading...");
		$.ajax({
			url: 'Servlet',
			type: 'post',
			datatype: 'json',
			data: $('#form2').serialize(),
			success: function(data){
				$("#dynamicText").html( JSON.parse(data) );
			},
			error: function(jqXHR,textStatus,errorThrown){
				$("#dynamicText").html(errorThrown);
			}
		});
	});
	
	
	$("#form3Submit").click(function() {
		$("#dynamicText").html("Loading...");
		$.ajax({
			url: 'Servlet',
			type: 'post',
			datatype: 'json',
			data: $('#form3').serialize(),
			success: function(data){
				var json = data.split("\n");
				var result = "";
				
				// Twitter user info
				var user = JSON.parse(json[0]);
				result += "<div class='tweet'>";
				result += "<img src='" + user.profileImageUrl + "' class='tweetImg' />";
				result += "<div class='tweetContent'><div class='tweetUser'>" + user.name + "</div>(@" + user.screenName + ")</div>";
				result += "</div>";
				
				// Venues
				data = JSON.parse(json[1]);
				$("#map-canvas").show();
				//$("dynamicText").html("<div id='map-canvas'></div>");
				var map = new google.maps.Map(document.getElementById("map-canvas"), {mapTypeId: google.maps.MapTypeId.ROADMAP});
				var bounds = new google.maps.LatLngBounds();
				
				$.each( data, function() {
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
				$("#dynamicText").html(result);
				map.fitBounds(bounds);
			},
			error: function(xhr,textStatus,errorThrown){
				$("#dynamicText").html(errorThrown);
			}
		});
	});
	
	$("#form4Submit").click(function() {
		var parameters = "formId=" + encodeURIComponent(document.getElementsByName("formId")[3].value);
		loadXMLDoc(parameters);
	});
	
	$(".results").on('click', '.getRetweets', function(e) {
		var tweet = $(this).parent().parent().parent().parent();
		var tweetId = tweet.find('.tweetId').val();
		
		$("#retweetsFor" + tweetId).html("Loading..");

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
					$("#retweetsFor" + tweetId).html(result);
				}
				
				$("[data-toggle='tooltip']").tooltip({ placement: 'bottom' });

			},
			error: function(xhr,textStatus,errorThrown){
				$("Error finding retweeters").insertAfter(tweet);
			}
		});
		
		e.preventDefault();
		
	});	
	
});