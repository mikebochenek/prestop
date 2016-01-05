
var app = angular.module("app", ["ngResource", "ngRoute", "ngAnimate"])
	.constant("apiUrl", "http://localhost:9000/api")
	.config(["$routeProvider", function($routeProvider) {
		return $routeProvider.when("/", {
			templateUrl: "/views/main",
			controller: "ListCtrl"
		}).when("/create", {
			templateUrl: "/views/detail",
			controller: "CreateCtrl"
	    }).when("/edit/:id", {
			templateUrl: "/views/detail",
			controller: "EditCtrl"
	    }).otherwise({
			redirectTo: "/"
		});
	}
	]).config([
	"$locationProvider", function($locationProvider) {
		return $locationProvider.html5Mode(true).hashPrefix("!"); // enable the new HTML5 routing and history API
	}
]);

// the global controller
app.controller("AppCtrl", ["$scope", "$location", function($scope, $location) {
	// the very sweet go function is inherited by all other controllers
	$scope.go = function (path) {
		$location.path(path);
	};
}]);

// MINE:  the list controller
app.controller("ListCtrl", ["$scope", "$resource", "$timeout", "apiUrl", 
                            function($scope, $resource, $timeout, apiUrl) {
	var date = new Date();

	$scope.datestring = function() {
		var month = date.getMonth() + 1;
		var day = date.getDate();
		return date.getFullYear() + (month < 10 ? "0" : "") + month + (day < 10 ? "0" : "") + day;
	}

	$scope.doneday = $scope.datestring();

	$scope.increasedate = function() {
		var result = new Date(date);
	    result.setDate(date.getDate() + 1);
	    date = result;
		$scope.doneday = $scope.datestring();
		var Celebrities = $resource(apiUrl + "/donelist/" + $scope.doneday); 
		$scope.celebrities = Celebrities.query(); 
	};

	$scope.decreasedate = function() {
		var result = new Date(date);
	    result.setDate(date.getDate() - 1); // increase and decrease are almost identical, except this line
	    date = result;
		$scope.doneday = $scope.datestring();
		var Celebrities = $resource(apiUrl + "/donelist/" + $scope.doneday); 
		$scope.celebrities = Celebrities.query(); 
	};

	$scope.init = function() {
		$scope.show=true;
	    var Celebrities = $resource(apiUrl + "/donelist/" + $scope.doneday); 
	    $scope.celebrities = Celebrities.query(); 
	};
	
	$scope.init();
	
	$scope.add = function() {
		var create = $resource(apiUrl + "/donelist/new"); // a RESTful-capable resource object
		create.save({'donetext' : $scope.donetext, 'doneday' : $scope.doneday}); 
		$scope.donetext=''; 
		$scope.show=false;
		$timeout(function() { $scope.init();  }, 300); // go back to public/html/main.html
		//TODO should I implement a success() or simply sleep for 500ms?
	};
}]);

// the create controller
app.controller("CreateCtrl", ["$scope", "$resource", "$timeout", "apiUrl", function($scope, $resource, $timeout, apiUrl) {
	// to save a celebrity
	$scope.save = function() {
		var CreateCelebrity = $resource(apiUrl + "/donelist/new"); // a RESTful-capable resource object
		CreateCelebrity.save($scope.celebrity); // $scope.celebrity comes from the detailForm in public/html/detail.html
		$timeout(function() { $scope.go('/'); }); // go back to public/html/main.html
	};
}]);

// the edit controller
app.controller("EditCtrl", ["$scope", "$resource", "$routeParams", "$timeout", "apiUrl", function($scope, $resource, $routeParams, $timeout, apiUrl) {
	var ShowCelebrity = $resource(apiUrl + "/celebrities/:id", {id:"@id"}); // a RESTful-capable resource object
	if ($routeParams.id) {
		// retrieve the corresponding celebrity from the database
		// $scope.celebrity.id.$oid is now populated so the Delete button will appear in the detailForm in public/html/detail.html
		$scope.celebrity = ShowCelebrity.get({id: $routeParams.id});
		$scope.dbContent = ShowCelebrity.get({id: $routeParams.id}); // this is used in the noChange function
	}
	
	// decide whether to enable or not the button Save in the detailForm in public/html/detail.html 
	$scope.noChange = function() {
		return angular.equals($scope.celebrity, $scope.dbContent);
	};

	// to update a celebrity
	$scope.save = function() {
		var UpdateCelebrity = $resource(apiUrl + "/celebrities/" + $routeParams.id); // a RESTful-capable resource object
		UpdateCelebrity.save($scope.celebrity); // $scope.celebrity comes from the detailForm in public/html/detail.html
		$timeout(function() { $scope.go('/'); }); // go back to public/html/main.html
	};
	
	// to delete a celebrity
	$scope.delete = function() {
		var DeleteCelebrity = $resource(apiUrl + "/celebrities/" + $routeParams.id); // a RESTful-capable resource object
		DeleteCelebrity.delete();
		$timeout(function() { $scope.go('/'); }); // go back to public/html/main.html
	};
}]);