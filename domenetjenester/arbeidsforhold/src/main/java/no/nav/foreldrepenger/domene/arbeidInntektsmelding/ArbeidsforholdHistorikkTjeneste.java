package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.historikk.VurderArbeidsforholdHistorikkinnslag;

@ApplicationScoped
class ArbeidsforholdHistorikkTjeneste {
    private HistorikkTjenesteAdapter historikkAdapter;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    ArbeidsforholdHistorikkTjeneste() {
        // CDI
    }

    @Inject
    ArbeidsforholdHistorikkTjeneste(HistorikkTjenesteAdapter historikkAdapter,
                                    ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;

    }

    public void opprettHistorikkinnslag(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef ref, AvklarPermisjonUtenSluttdatoDto avklartArbForhold) {

        var arbeidsforholdNavn = lagTekstMedArbeidsgiverOgArbeidforholdRef(arbeidsgiver, ref);
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

    String lagTekstMedArbeidsgiverOgArbeidforholdRef(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internArbeidsforholdRef) {
        var sb = new StringBuilder();
        //Usikker på denne - vil man ikke alltid ha arbeidsgivernavn?
        if ((arbeidsgiver != null) && (internArbeidsforholdRef != null) && internArbeidsforholdRef.gjelderForSpesifiktArbeidsforhold()) {
            return lagTekstForArbeidsforholdRef(internArbeidsforholdRef, sb);
        }
        if (arbeidsgiver != null) {
            return lagTekstForArbeidsgiver(arbeidsgiver, sb);
        }
        throw new IllegalStateException("Klarte ikke lage historikkinnslagstekst for arbeidsgiver");
    }

    private String lagTekstForArbeidsgiver(Arbeidsgiver arbeidsgiver, StringBuilder sb) {
        var opplysninger =  arbeidsgiverTjeneste.hent(arbeidsgiver);
        sb.append(opplysninger.getNavn())
            .append(" (")
            .append(opplysninger.getIdentifikator())
            .append(")");
        return sb.toString();
    }

    private String lagTekstForArbeidsforholdRef(InternArbeidsforholdRef internArbeidsforholdRef, StringBuilder sb) {
        var referanse = internArbeidsforholdRef.getReferanse();
        var sisteFireTegnIRef = referanse.substring(referanse.length() - 4);
        sb.append(" ...")
            .append(sisteFireTegnIRef);
        return sb.toString();
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
}
