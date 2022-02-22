package no.nav.foreldrepenger.domene.arbeidInntektsmelding.historikk;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.AvklarPermisjonUtenSluttdatoDto;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.historikk.VurderArbeidsforholdHistorikkinnslag;

@ApplicationScoped
public class ArbeidPermHistorikkInnslagTjeneste {
    private HistorikkTjenesteAdapter historikkAdapter;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    ArbeidPermHistorikkInnslagTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidPermHistorikkInnslagTjeneste(HistorikkTjenesteAdapter historikkAdapter, ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;

    }

    public void opprettHistorikkinnslag(AvklarPermisjonUtenSluttdatoDto avklartArbForhold) {
        Arbeidsgiver arbeidsgiver = lagArbeidsgiver(avklartArbForhold.arbeidsgiverIdent());

        var arbeidsforholdNavn = lagTekstForArbeidsgiver(arbeidsgiver);
        var historikkInnslagType = utledHistorikkInnslagValg(avklartArbForhold.permisjonStatus());

        opprettHistorikkinnslagDel(historikkInnslagType, arbeidsforholdNavn, arbeidsforholdNavn);
    }

    private VurderArbeidsforholdHistorikkinnslag utledHistorikkInnslagValg(BekreftetPermisjonStatus permisjonStatus) {
        if (BekreftetPermisjonStatus.IKKE_BRUK_PERMISJON.equals(permisjonStatus)) {
           return  VurderArbeidsforholdHistorikkinnslag.SØKER_ER_IKKE_I_PERMISJON;
        } else if (BekreftetPermisjonStatus.BRUK_PERMISJON.equals(permisjonStatus)) {
            return VurderArbeidsforholdHistorikkinnslag.SØKER_ER_I_PERMISJON;
        } else return null;
    }

    String lagTekstForArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver == null) {
            throw new IllegalStateException("Klarte ikke lage historikkinnslagstekst for arbeidsgiver");
        } else {
            var sb = new StringBuilder();
            var opplysninger =  arbeidsgiverTjeneste.hent(arbeidsgiver);
            sb.append(opplysninger.getNavn())
                .append(" (")
                .append(opplysninger.getIdentifikator())
                .append(")");
            return sb.toString();
        }
    }

    private void opprettHistorikkinnslagDel(VurderArbeidsforholdHistorikkinnslag tilVerdi, String begrunnelse, String arbeidsforholdNavn) {
        historikkAdapter.tekstBuilder().ferdigstillHistorikkinnslagDel();
        var historikkDeler = historikkAdapter.tekstBuilder().getHistorikkinnslagDeler();
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

    private Arbeidsgiver lagArbeidsgiver(String arbeidsgiverIdent) {
        if (OrgNummer.erGyldigOrgnr(arbeidsgiverIdent)) {
            return Arbeidsgiver.virksomhet(arbeidsgiverIdent);
        }
        return Arbeidsgiver.fra(new AktørId(arbeidsgiverIdent));
    }
}
