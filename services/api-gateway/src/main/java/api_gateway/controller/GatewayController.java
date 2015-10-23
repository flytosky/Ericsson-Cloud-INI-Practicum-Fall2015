package api_gateway.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import rx.Observable;
import rx.Observer;
import api_gateway.models.MovieDetails;
import api_gateway.models.MovieDetailsList;
import api_gateway.services.images.ImageIntegrationService;
import api_gateway.services.movie.MovieIntegrationService;
import api_gateway.services.ratings.RatingIntegrationService;
import api_gateway.services.similar_movie.SimilarMovieIntegrationService;

@RestController
@RequestMapping("/movie")
public class GatewayController {

	@Autowired
	MovieIntegrationService movieIntegrationService;

	@Autowired
	RatingIntegrationService ratingIntegrationService;

	@Autowired
	ImageIntegrationService imageIntegrationService;

	@Autowired
	SimilarMovieIntegrationService similarMovieIntegrationService;

	@RequestMapping(value = "{mID}", method = RequestMethod.GET)
	public DeferredResult<MovieDetails> getMovieDetails(@PathVariable String mID) {
		Observable<MovieDetails> details = Observable.zip(movieIntegrationService.getMovie(mID),
				ratingIntegrationService.ratingFor(mID), imageIntegrationService.imageFor(mID),
				similarMovieIntegrationService.getSimilarMovie(mID), (movie, ratings, image, similars) -> {
					MovieDetails movieDetails = new MovieDetails();
					movieDetails.setMovie(movie);
					movieDetails.setRatings(ratings);
					movieDetails.setSimilars(similars);
					movieDetails.setImage(image);
					return movieDetails;
				});
		return toDeferredResult(details);
	}

	public DeferredResult<MovieDetails> toDeferredResult(Observable<MovieDetails> details) {
		DeferredResult<MovieDetails> result = new DeferredResult<>();
		details.subscribe(new Observer<MovieDetails>() {
			@Override
			public void onCompleted() {
			}

			@Override
			public void onError(Throwable throwable) {
			}

			@Override
			public void onNext(MovieDetails movieDetails) {
				result.setResult(movieDetails);
			}
		});
		return result;
	}

	@RequestMapping(value = "/latest/{n}", method = RequestMethod.GET)
	public DeferredResult<MovieDetailsList> getMovieListDetails(@PathVariable String n) {

		Observable<MovieDetailsList> details = Observable.zip(
				movieIntegrationService.getMovieList(n),
				ratingIntegrationService.getRatingList(n), 
				imageIntegrationService.getImageList(n),
				similarMovieIntegrationService.getSimilarMovieList(n), 
				(movie, ratings, image, similars) -> {
					List<MovieDetails> list = new ArrayList<MovieDetails>();
					for (int i = 0; i < Integer.parseInt(n); i++) {
						MovieDetails movieDetails = new MovieDetails();
						movieDetails.setMovie(movie.getMovie().get(i));
						movieDetails.setRatings(ratings.getRatingList());
						movieDetails.setSimilars(similars.getSimilarMovieList().get(i));
						movieDetails.setImage(image.getList().get(i));
						list.add(movieDetails);
					}
					MovieDetailsList movieDetailsList = new MovieDetailsList();
					movieDetailsList.setMovie(list);
					return movieDetailsList;
				});

		return toDeferredListResult(details);
	}

	public DeferredResult<MovieDetailsList> toDeferredListResult(Observable<MovieDetailsList> details) {
		DeferredResult<MovieDetailsList> result = new DeferredResult<>();
		details.subscribe(new Observer<MovieDetailsList>() {
			@Override
			public void onCompleted() {
			}

			@Override
			public void onError(Throwable throwable) {
			}

			@Override
			public void onNext(MovieDetailsList movieDetails) {
				result.setResult(movieDetails);
			}
		});
		return result;
	}
}
