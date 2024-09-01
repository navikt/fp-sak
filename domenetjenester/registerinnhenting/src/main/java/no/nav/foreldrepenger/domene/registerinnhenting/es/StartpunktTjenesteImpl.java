package no.nav.foreldrepenger.domene.registerinnhenting.es;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.arbeidsforhold.IAYGrunnlagDiff;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktTjeneste;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktUtleder;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
public class StartpunktTjenesteImpl implements StartpunktTjeneste {

    private Instance<StartpunktUtleder> utledere;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Inject
    public StartpunktTjenesteImpl(@Any Instance<StartpunktUtleder> utledere,
                                  FamilieHendelseTjeneste familieHendelseTjeneste,
                                  InntektArbeidYtelseTjeneste iayTjeneste) {
        this.utledere = utledere;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.iayTjeneste = iayTjeneste;
    }

    StartpunktTjenesteImpl() {
        // CDI
    }

    @Override
    public StartpunktType utledStartpunktMotOriginalBehandling(BehandlingReferanse revurdering, Skjæringstidspunkt stp) {
        throw new IllegalStateException("Utviklerfeil: Skal ikke kalle startpunkt mot original for Engangsstønad, sak: " + revurdering.saksnummer().getVerdi());
    }

    @Override
    public StartpunktType utledStartpunktForDiffBehandlingsgrunnlag(BehandlingReferanse revurdering, Skjæringstidspunkt stp, EndringsresultatDiff differanse) {
        var startpunkt = utledStartpunkterES(revurdering, stp, differanse).stream()
            .min(Comparator.comparing(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
        return StartpunktType.inngangsVilkårStartpunkt().contains(startpunkt) ? startpunkt : StartpunktType.UDEFINERT;
    }

    private List<StartpunktType> utledStartpunkterES(BehandlingReferanse revurdering, Skjæringstidspunkt stp, EndringsresultatDiff differanse) {
        List<StartpunktType> startpunkter = new ArrayList<>();
        var grunnlagForBehandling = familieHendelseTjeneste.hentAggregat(revurdering.behandlingId());
        if (skalSjekkeForManglendeFødsel(grunnlagForBehandling))
            startpunkter.add(StartpunktType.SØKERS_RELASJON_TIL_BARNET);

        differanse.hentDelresultat(FamilieHendelseGrunnlagEntitet.class).filter(EndringsresultatDiff::erSporedeFeltEndret).ifPresent(diff -> {
            if (erAntallBekreftedeBarnEndret((long) diff.getGrunnlagId1(), (long) diff.getGrunnlagId2()))
                startpunkter.add(StartpunktType.SØKERS_RELASJON_TIL_BARNET);
        });
        differanse.hentDelresultat(MedlemskapAggregat.class).filter(EndringsresultatDiff::erSporedeFeltEndret)
            .ifPresent(diff -> startpunkter.add(StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP));
        differanse.hentDelresultat(PersonInformasjonEntitet.class).filter(EndringsresultatDiff::erSporedeFeltEndret)
            .ifPresent(diff -> startpunkter.add(utledStartpunktForDelresultat(revurdering, stp, diff)));
        differanse.hentDelresultat(InntektArbeidYtelseGrunnlag.class).filter(EndringsresultatDiff::erSporedeFeltEndret).ifPresent(diff -> {
            var grunnlag1 = iayTjeneste.hentGrunnlagPåId(revurdering.behandlingId(), (UUID)diff.getGrunnlagId1());
            var grunnlag2 = iayTjeneste.hentGrunnlagPåId(revurdering.behandlingId(), (UUID)diff.getGrunnlagId2());
            var iayGrunnlagDiff = new IAYGrunnlagDiff(grunnlag1, grunnlag2);
            var aktørYtelseEndringForSøker = iayGrunnlagDiff
                .endringPåAktørYtelseForAktør(revurdering.saksnummer(), stp.getUtledetSkjæringstidspunkt(), revurdering.aktørId());
            if (aktørYtelseEndringForSøker.erEksklusiveYtelserEndret())
                startpunkter.add(StartpunktType.SØKERS_RELASJON_TIL_BARNET);
        });
        return startpunkter;
    }

    private StartpunktType utledStartpunktForDelresultat(BehandlingReferanse revurdering, Skjæringstidspunkt stp, EndringsresultatDiff diff) {
        var utleder = GrunnlagRef.Lookup.find(StartpunktUtleder.class, utledere, diff.getGrunnlag()).orElseThrow();
        return diff.erSporedeFeltEndret() ?
            utleder.utledStartpunkt(revurdering, stp, diff.getGrunnlagId1(), diff.getGrunnlagId2()) : StartpunktType.UDEFINERT;
    }

    private boolean erAntallBekreftedeBarnEndret(Long id1, Long id2) {
        var grunnlag1 = familieHendelseTjeneste.hentGrunnlagPåId(id1);
        var grunnlag2 = familieHendelseTjeneste.hentGrunnlagPåId(id2);
        var antallBarn1 = grunnlag1.getGjeldendeVersjon().getAntallBarn();
        var antallBarn2 = grunnlag2.getGjeldendeVersjon().getAntallBarn();

        return !antallBarn1.equals(antallBarn2);
    }

    private boolean skalSjekkeForManglendeFødsel(FamilieHendelseGrunnlagEntitet grunnlagForBehandling) {
        return FamilieHendelseTjeneste.getManglerFødselsRegistreringFristUtløpt(grunnlagForBehandling);
    }

}
