package no.nav.foreldrepenger.jsonfeed.dto;

import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.FeedElement;

import java.util.List;

public class VedtakDto {
    private boolean harFlereElementer;
    private List<FeedElement> elementer;

    public VedtakDto(boolean harFlereElementer, List<FeedElement> elementer) {
        super();
        this.harFlereElementer = harFlereElementer;
        this.elementer = elementer;
    }

    public boolean isHarFlereElementer() {
        return harFlereElementer;
    }

    public List<FeedElement> getElementer() {
        return elementer;
    }




}
