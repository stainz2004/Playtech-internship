package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;


public class Main {
    private static final String MY_CATEGORY = "DIY";

    private long totalBudget;
    private long remainingBudget;

    private double competitionFactor = 1.0;


    public Main(long budget) {
        this.totalBudget = Math.max(0, budget);
        this.remainingBudget = this.totalBudget;
    }

    private void run() throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter out = new PrintWriter(System.out, true);

        out.println(MY_CATEGORY);

        String line;
        while ((line = in.readLine()) != null) {
            if (line.isEmpty()) continue;

            char c0 = line.charAt(0);

            if (c0 == 'W') {
                long spent = Long.parseLong(line.substring(2).trim());
                remainingBudget = Math.max(0, remainingBudget - spent);
            } else if (c0 == 'L') {
            } else if (c0 == 'S') {
                onSummary(line);

            } else {

                Bid bid = computeBid(line);
                out.println(bid.startBid + " " + bid.maxBid);
            }
        }
    }

    private void onSummary(String line) {
        String[] items = line.split(" ");
        int points = Integer.parseInt(items[1]);
        int creditsSpent = Integer.parseInt(items[2]);

        if (points <= 20) {
            competitionFactor *= 1.2;
            return;
        }

        if (creditsSpent <= 200) {
            competitionFactor *= 1.1;
        } else if (creditsSpent <= 600) {
            competitionFactor *= 1.05;
        } else if (creditsSpent <= 850) {
            competitionFactor *= 0.95;
        } else if (creditsSpent <= 1200) {
            competitionFactor *= 0.90;
        } else {
            competitionFactor *= 0.85;
        }

        if (competitionFactor < 0.7) competitionFactor = 0.7;
    }

    private Bid computeBid(String payload) {
        if (remainingBudget <= 0) {
            return new Bid(0, 0);
        }

        Parsed x = parseText(payload);

        int utility = 0;
        if (MY_CATEGORY.equals(x.videoCategory)) utility += 10;
        if (x.subscribed) utility += 1;
        utility += ageScore(x.age);
        utility += interestScore(x.interests, x.videoCategory);
        utility += viewCountScore(x.viewCount);
        utility += commentRatioScore(x.viewCount, x.commentCount);

        int baseBid = baseMaxBidForUtility(utility);

        int maxBid = (int) Math.round(baseBid * competitionFactor);

        maxBid = Math.min(maxBid, (int) remainingBudget);
        if (maxBid < 1) maxBid = 1;

        int startBid;
        if (utility >= 15) startBid = (int) Math.max(1, Math.round(maxBid * 0.53));
        else if (utility >= 12) startBid = (int) Math.max(1, Math.round(maxBid * 0.45));
        else if (utility >= 9)  startBid = (int) Math.max(1, Math.round(maxBid * 0.36));
        else if (utility >= 6)  startBid = (int) Math.max(1, Math.round(maxBid * 0.30));
        else                    startBid = (int) Math.max(1, Math.round(maxBid * 0.25));

        return new Bid(startBid, maxBid);
    }

    private static int ageScore(String age) {
        if (age == null) return 0;
        return switch (age) {
            case "18-24" -> 3;
            case "25-34" -> 4;
            case "35-44" -> 3;
            case "13-17" -> 1;
            case "45-54" -> 1;
            case "55+" -> 1;
            default -> 1;
        };
    }

    private static int interestScore(String interests, String videoCategory) {
        if (interests == null || interests.isEmpty()) return 0;

        int score = 0;
        String[] parts = interests.split(";");
        for (String interest : parts) {
            if (interest.equals(MY_CATEGORY)) {
                score += 3;
            }
            if (interest.equals(videoCategory)) {
                score += 2;
            }
        }
        return score;
    }

    private static int viewCountScore(long viewCount) {
        if (viewCount < 1000) return 0;
        if (viewCount < 10000) return 1;
        if (viewCount < 100000) return 3;
        if (viewCount < 1000000) return 4;
        if (viewCount < 10000000) return 2;
        return 1;
    }

    private static int commentRatioScore(long views, long comments) {
        if (views <= 0) return 0;

        double ratio = (double) comments / (double) views;

        if (ratio > 0.020) return 4;
        if (ratio > 0.010) return 3;
        if (ratio > 0.005) return 2;
        if (ratio > 0.001) return 1;
        return 0;
    }

    private int baseMaxBidForUtility(int utility) {
        if (utility >= 18) return 82;
        if (utility >= 15) return 62;
        if (utility >= 12) return 46;
        if (utility >= 9)  return 31;
        if (utility >= 6)  return 21;
        if (utility >= 3)  return 11;
        return 6;
    }

    private static Parsed parseText(String line) {
        Parsed p = new Parsed();

        String[] items = line.split(",");
        for (String kv : items) {
            int eq = kv.indexOf('=');
            if (eq <= 0 || eq + 1 >= kv.length()) continue;

            String key = kv.substring(0, eq).trim();
            String val = kv.substring(eq + 1).trim();

            switch (key) {
                case "video.category":
                    p.videoCategory = val;
                    break;
                case "video.viewCount":
                    p.viewCount = Long.parseLong(val);
                    break;
                case "video.commentCount":
                    p.commentCount = Long.parseLong(val);
                    break;
                case "viewer.subscribed":
                    p.subscribed = "Y".equals(val);
                    break;
                case "viewer.age":
                    p.age = val;
                    break;
                case "viewer.interests":
                    p.interests = val;
                    break;
                default:
                    break;
            }
        }

        return p;
    }

    private static final class Parsed {
        String videoCategory = "";
        long viewCount = 0;
        long commentCount = 0;
        boolean subscribed = false;
        String age = "";
        String interests = "";
    }


    private static final class Bid {
        final int startBid;
        final int maxBid;

        Bid(int startBid, int maxBid) {
            this.startBid = startBid;
            this.maxBid = maxBid;
        }
    }


    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            // Keep stderr logs allowed by protocol.
            System.err.println("Expected one arg: total ebucks");
            return;
        }

        long budget;
        try {
            budget = Long.parseLong(args[0].trim());
        } catch (NumberFormatException nfe) {
            System.err.println("Invalid budget: " + args[0]);
            return;
        }

        Main bot = new Main(budget);
        bot.run();
    }
}
