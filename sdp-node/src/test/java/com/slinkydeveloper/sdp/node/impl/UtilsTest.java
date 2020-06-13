package com.slinkydeveloper.sdp.node.impl;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

    @Test
    void generateNextNeighboursWhereMyselfIsInMiddle() {
        Set<Integer> input = new HashSet<>(Arrays.asList(
                1, 2, 4, 5
        ));

        List<Integer> output = Arrays.asList(4, 5, 1, 2);

        assertThat(Utils.generateNextNeighboursList(input, 3))
                .isEqualTo(output);
    }

    @Test
    void generateNextNeighboursListWhereMyselfIsFirst() {
        Set<Integer> input = new HashSet<>(Arrays.asList(
                2, 3, 4, 5
        ));

        List<Integer> output = Arrays.asList(2, 3, 4, 5);

        assertThat(Utils.generateNextNeighboursList(input, 1))
                .isEqualTo(output);
    }

    @Test
    void generateNextNeighboursListWhereMyselfIsLast() {
        Set<Integer> input = new HashSet<>(Arrays.asList(
                1, 2, 3, 4, 5
        ));

        List<Integer> output = Arrays.asList(1, 2, 3, 4);

        assertThat(Utils.generateNextNeighboursList(input, 5))
                .isEqualTo(output);
    }

    @Test
    void generateNextNeighboursListWithMissing() {
        Set<Integer> input = new HashSet<>(Arrays.asList(
                2, 5
        ));

        List<Integer> output = Arrays.asList(5, 2);

        assertThat(Utils.generateNextNeighboursList(input, 3))
                .isEqualTo(output);
    }

    @Test
    void generateNextNeighboursShuffled() {
        Set<Integer> input = new HashSet<>(Arrays.asList(
            2, 4, 1
        ));

        List<Integer> output = Arrays.asList(4, 1, 2);

        assertThat(Utils.generateNextNeighboursList(input, 3))
            .isEqualTo(output);
    }
}
