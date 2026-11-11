/*
 * Copyright (C) 2016-2025 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders;

import java.util.HashMap;
import java.util.Map;

public class WishList {

    private final Map<String, Integer> wishes = new HashMap<>();

    public void addWish(String wish) {
        if (wishes.containsKey(wish)) {
            int n = wishes.get(wish) + 1;
            wishes.put(wish, n);
        } else {
            wishes.put(wish, 1);
        }
    }

    public void removeWish(String wish) {
        if (wishes.containsKey(wish)) {
            int n = wishes.get(wish) - 1;
            if (n == 0) {
                wishes.remove(wish);
            } else {
                wishes.put(wish, n);
            }
        } else {
            throw new IllegalStateException("Wish does not exist and cannot be removed.");
        }
    }

    public int numberOfWishesFor(String wish) {
        return wishes.getOrDefault(wish, 0);
    }

    public int numberOfWishes() {
        int sum = 0;
        for (int wishes : wishes.values()) {
            sum += wishes;
        }
        return sum;
    }

    public String mostWished() {
        String mostWishedWish = null;
        int mostWishes = 0;

        for (Map.Entry<String, Integer> wish : wishes.entrySet()) {
            if (wish.getValue() > mostWishes) {
                mostWishedWish = wish.getKey();
                mostWishes = wish.getValue();
            }
        }

        return mostWishedWish;
    }

}
