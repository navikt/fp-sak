package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;


import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.jackson.JacksonJsonConfig;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.FastsettePeriodeResultat;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.FastsettePerioderRegelOrkestrering;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.RegelGrunnlag;

@ApplicationScoped
public class FastsettePerioderRegelAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(FastsettePerioderRegelAdapter.class);
    private static final FastsettePerioderRegelOrkestrering REGEL = new FastsettePerioderRegelOrkestrering();

    private final JacksonJsonConfig jackson = new JacksonJsonConfig();

    private FastsettePerioderRegelGrunnlagBygger regelGrunnlagBygger;
    private FastsettePerioderRegelResultatKonverterer regelResultatKonverterer;

    FastsettePerioderRegelAdapter() {
        // For CDI
    }

    @Inject
    public FastsettePerioderRegelAdapter(FastsettePerioderRegelGrunnlagBygger regelGrunnlagBygger,
                                         FastsettePerioderRegelResultatKonverterer regelResultatKonverterer) {
        this.regelGrunnlagBygger = regelGrunnlagBygger;
        this.regelResultatKonverterer = regelResultatKonverterer;
    }

    UttakResultatPerioderEntitet fastsettePerioder(UttakInput input) {
        var grunnlag = regelGrunnlagBygger.byggGrunnlag(input);
        List<FastsettePeriodeResultat> resultat;
        try {
            resultat = REGEL.fastsettePerioder(grunnlag);
        } catch (Exception e) {
            log(grunnlag);
            throw new RuntimeException("Automatisk fastsetting av uttak feilet", e);
        }
        return regelResultatKonverterer.konverter(input, resultat);
    }

    private void log(RegelGrunnlag grunnlag) {
        try {
            LOG.info("Fastsette uttaksperioder feilet med grunnlag {}", jackson.toJson(grunnlag)
                .replaceAll("\\d{13}|\\d{11}|\\d{9}", "*"));
        } catch (JsonProcessingException jsonProcessingException) {
            LOG.warn("Feil ved logging av regelgrunnlaget", jsonProcessingException);
        }
    }
}









