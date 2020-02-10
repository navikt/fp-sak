package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.historikk.VurderArbeidsforholdHistorikkinnslag;

@ApplicationScoped
class ArbeidsforholdHistorikkinnslagTjeneste {

    private HistorikkTjenesteAdapter historikkAdapter;
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;

    ArbeidsforholdHistorikkinnslagTjeneste() {
        // CDI
    }

    @Inject
    ArbeidsforholdHistorikkinnslagTjeneste(HistorikkTjenesteAdapter historikkAdapter,
                                           ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
    }

    public void opprettHistorikkinnslag(AksjonspunktOppdaterParameter param, ArbeidsforholdDto arbeidsforholdDto, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef ref, List<ArbeidsforholdOverstyring> overstyringer) {
        final LocalDate stp = param.getSkjæringstidspunkt().getUtledetSkjæringstidspunkt();
        String arbeidsforholdNavn = arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(arbeidsgiver, ref, overstyringer);
        opprettHistorikkinnslag(arbeidsforholdDto, arbeidsforholdNavn, Optional.of(stp));
    }

    public void opprettHistorikkinnslag(ArbeidsforholdDto arbeidsforholdDto, String arbeidsforholdNavn, Optional<LocalDate> stpOpt){
        List<VurderArbeidsforholdHistorikkinnslag> historikkinnslagKoder = utledKoderForHistorikkinnslagdeler(arbeidsforholdDto, stpOpt);
        historikkinnslagKoder.forEach(kode -> opprettHistorikkinnslagDel(kode, arbeidsforholdDto.getBegrunnelse(), arbeidsforholdNavn));
    }

    private List<VurderArbeidsforholdHistorikkinnslag> utledKoderForHistorikkinnslagdeler(ArbeidsforholdDto arbeidsforholdDto, Optional<LocalDate> stpOpt) {
        if (Boolean.FALSE.equals(arbeidsforholdDto.getBrukArbeidsforholdet())) {
            return List.of(VurderArbeidsforholdHistorikkinnslag.IKKE_BRUK);
        }
        if (Boolean.TRUE.equals(arbeidsforholdDto.getBrukArbeidsforholdet())){
            return utledKoderForHistorikkinnslagdelerForArbeidsforholdSomSkalBrukes(arbeidsforholdDto, stpOpt);
        }
        return List.of();
    }

    private List<VurderArbeidsforholdHistorikkinnslag> utledKoderForHistorikkinnslagdelerForArbeidsforholdSomSkalBrukes(ArbeidsforholdDto arbeidsforholdDto, Optional<LocalDate> stpOpt) {
        List<VurderArbeidsforholdHistorikkinnslag> list = new ArrayList<>();
        UtledKoderForHistorikkinnslagdelerForArbeidsforholdMedPermisjon.utled(arbeidsforholdDto).ifPresent(list::add);
        UtledKoderForHistorikkinnslagdelerForNyttEllerErstattetArbeidsforhold.utled(arbeidsforholdDto).ifPresent(list::add);
        if (UtledOmHistorikkinnslagForInntektsmeldingErNødvendig.utled(arbeidsforholdDto, stpOpt)) {
            UtledKoderForHistorikkinnslagdelerForArbeidsforholdUtenInnteksmelding.utled(arbeidsforholdDto).ifPresent(list::add);
        }
        return list;
    }

    private void opprettHistorikkinnslagDel(VurderArbeidsforholdHistorikkinnslag tilVerdi, String begrunnelse, String arbeidsforholdNavn) {
        historikkAdapter.tekstBuilder().ferdigstillHistorikkinnslagDel();
        List<HistorikkinnslagDel> historikkDeler = historikkAdapter.tekstBuilder().getHistorikkinnslagDeler();
        historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.ARBEIDSFORHOLD, arbeidsforholdNavn, null, tilVerdi);
        historikkAdapter.tekstBuilder().medBegrunnelse(begrunnelse);
        if (!harSkjermlenke(historikkDeler)) {
            historikkAdapter.tekstBuilder().medSkjermlenke(SkjermlenkeType.FAKTA_OM_ARBEIDSFORHOLD);
        }
        historikkAdapter.tekstBuilder().ferdigstillHistorikkinnslagDel();
    }

    private boolean harSkjermlenke(List<HistorikkinnslagDel> historikkDeler) {
        return historikkDeler.stream().anyMatch(historikkDel -> historikkDel.getSkjermlenke().isPresent());
    }

}
