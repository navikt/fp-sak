package no.nav.foreldrepenger.domene.registerinnhenting.svp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
public class StartpunktTjenesteImpl implements StartpunktTjeneste {

    @Inject
    public StartpunktTjenesteImpl() {
        // Plattform trenger tom Ctor (Hibernate, CDI, etc)
    }

    @Override
    public StartpunktType utledStartpunktMotOriginalBehandling(BehandlingReferanse revurdering) {
        throw new IllegalStateException("Denne metoden er ikke implementert for SVP");
    }

    @Override
    public StartpunktType utledStartpunktForDiffBehandlingsgrunnlag(BehandlingReferanse revurdering, EndringsresultatDiff differanse) {
        var sporedeFeltEndret = differanse.hentDelresultater().stream().anyMatch(EndringsresultatDiff::erSporedeFeltEndret);
        var startpunktType = sporedeFeltEndret ?
            StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT :
            StartpunktType.UDEFINERT;
        var iayGrunnlagDiffOpt = differanse.hentDelresultat(InntektArbeidYtelseGrunnlag.class);
        if (iayGrunnlagDiffOpt.map(EndringsresultatDiff::erSporedeFeltEndret).orElse(Boolean.FALSE)) {
            startpunktType = StartpunktType.KONTROLLER_ARBEIDSFORHOLD;
        }
        return startpunktType;
    }
}
