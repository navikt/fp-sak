package no.nav.foreldrepenger.domene.arbeidInntektsmelding.historikk;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder.fraTilEquals;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.AvklarPermisjonUtenSluttdatoDto;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.historikk.VurderArbeidsforholdHistorikkinnslag;

@ApplicationScoped
public class ArbeidPermHistorikkInnslagTjeneste {
    private Historikkinnslag2Repository historikkinnslagRepository;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    ArbeidPermHistorikkInnslagTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidPermHistorikkInnslagTjeneste(Historikkinnslag2Repository historikkinnslagRepository, ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }


    public void opprettHistorikkinnslag(BehandlingReferanse ref, List<AvklarPermisjonUtenSluttdatoDto> avklarteArbForhold, String begrunnelse) {
        var historikkinnslag = new Historikkinnslag2.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(ref.fagsakId())
            .medBehandlingId(ref.behandlingId())
            .medTittel(SkjermlenkeType.FAKTA_OM_ARBEIDSFORHOLD_PERMISJON)
            .medTekstlinjer(lagEndredeTekstlinjerForHvertAvklartArbeidsforhold(avklarteArbForhold))
            .addTekstlinje(begrunnelse)
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    private List<HistorikkinnslagTekstlinjeBuilder> lagEndredeTekstlinjerForHvertAvklartArbeidsforhold(List<AvklarPermisjonUtenSluttdatoDto> avklarteArbForhold) {
        return avklarteArbForhold.stream()
            .map(this::tilTekstlinje)
            .toList();
    }

    private HistorikkinnslagTekstlinjeBuilder tilTekstlinje(AvklarPermisjonUtenSluttdatoDto avklartArbForhold) {
        var arbeidsforholdNavn = arbeisdforholdNavnFra(avklartArbForhold);
        var historikkInnslagType = utledHistorikkInnslagValg(avklartArbForhold.permisjonStatus());
        return fraTilEquals(String.format("Arbeidsforhold hos %s", arbeidsforholdNavn), null, historikkInnslagType);
    }

    private String arbeisdforholdNavnFra(AvklarPermisjonUtenSluttdatoDto avklartArbForhold) {
        var arbeidsgiver = lagArbeidsgiver(avklartArbForhold.arbeidsgiverIdent());
        var opplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
        return ArbeidsgiverHistorikkinnslag.lagArbeidsgiverHistorikkinnslagTekst(opplysninger, Optional.empty());
    }

    private static VurderArbeidsforholdHistorikkinnslag utledHistorikkInnslagValg(BekreftetPermisjonStatus permisjonStatus) {
        if (BekreftetPermisjonStatus.IKKE_BRUK_PERMISJON.equals(permisjonStatus)) {
           return  VurderArbeidsforholdHistorikkinnslag.SØKER_ER_IKKE_I_PERMISJON;
        } else if (BekreftetPermisjonStatus.BRUK_PERMISJON.equals(permisjonStatus)) {
            return VurderArbeidsforholdHistorikkinnslag.SØKER_ER_I_PERMISJON;
        } else return null;
    }

    private static Arbeidsgiver lagArbeidsgiver(String arbeidsgiverIdent) {
        if (OrgNummer.erGyldigOrgnr(arbeidsgiverIdent)) {
            return Arbeidsgiver.virksomhet(arbeidsgiverIdent);
        }
        return Arbeidsgiver.fra(new AktørId(arbeidsgiverIdent));
    }
}
