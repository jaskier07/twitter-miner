package pl.kania.trendminer.dataparser.preproc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import pl.kania.trendminer.dataparser.Tweet;
import pl.kania.trendminer.dataparser.input.CsvReader;
import pl.kania.trendminer.dataparser.input.TweetAnalysisData;
import pl.kania.trendminer.dataparser.preproc.replacing.TweetContentPreprocessor;
import pl.kania.trendminer.dataparser.preproc.filtering.ValidEnglishWordThresholdProvider;
import pl.kania.trendminer.dataparser.preproc.filtering.ValidEnglishWordsCounter;
import pl.kania.trendminer.util.NumberFormatter;
import pl.kania.trendminer.util.ProgressLogger;

import java.util.Iterator;
import java.util.List;

@Slf4j
@Service
public class Receiver {

    private final ValidEnglishWordsCounter validEnglishWordsCounter;
    private final ValidEnglishWordThresholdProvider validEnglishWordThresholdProvider;
    private final Environment environment;

    public Receiver(@Autowired ValidEnglishWordsCounter validEnglishWordsCounter, @Autowired ValidEnglishWordThresholdProvider validEnglishWordThresholdProvider,
                    @Autowired Environment environment) {
        this.validEnglishWordsCounter = validEnglishWordsCounter;
        this.validEnglishWordThresholdProvider = validEnglishWordThresholdProvider;
        this.environment = environment;
    }

    public TweetAnalysisData getTweetsInEnglish() {
        TweetAnalysisData tweetAnalysisData = new CsvReader().readFile(environment.getProperty("pl.kania.path.dataset"));
        List<Tweet> tweets = tweetAnalysisData.getTweets();
        performPreprocessing(tweets);
        filterOutNonEnglishTweets(tweets);
        return new TweetAnalysisData(tweets, tweetAnalysisData.getStart(), tweetAnalysisData.getEnd());
    }

    private void performPreprocessing(List<Tweet> tweets) {
        tweets.forEach(t -> new TweetContentPreprocessor().performPreprocessing(t));
    }

    private void filterOutNonEnglishTweets(List<Tweet> tweets) {
        log.info("Filtering non-English tweets started.");
        int tweetsBeforeFilteringOut = tweets.size();

        int counter = 0;
        Iterator<Tweet> iterator = tweets.iterator();
        while (iterator.hasNext()) {
            Tweet tweet = iterator.next();
            int percentageOfEnglishWords = validEnglishWordsCounter.getPercentageOfEnglishWords(tweet);
            if (percentageOfEnglishWords < validEnglishWordThresholdProvider.getThresholdInPercentage(tweet)) {
                iterator.remove();
                log.debug("Removed non-English tweet: " + tweet.getContent());
            }
            ProgressLogger.log(counter++);
        }

        ProgressLogger.done("Filtering non-English tweets. % of removed tweets: " +
                NumberFormatter.formatPercentage(tweets.size(), tweetsBeforeFilteringOut));
        log.info("Percentage of tweets with location: " + NumberFormatter.formatPercentage(
                ValidEnglishWordThresholdProvider.getTweetsWithLocation(), tweetsBeforeFilteringOut));
    }
}
