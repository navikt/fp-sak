package no.nav.foreldrepenger.domene.rest.historikk;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Organisasjonstype;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningAktivitetNøkkel;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@ApplicationScoped
public class ArbeidsgiverHistorikkinnslag {
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    ArbeidsgiverHistorikkinnslag() {
        // CDI
    }

    @Inject
    public ArbeidsgiverHistorikkinnslag(ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }

    private String lagArbeidsgiverHistorikkinnslagTekst(Arbeidsgiver arbeidsgiver,
                                                        InternArbeidsforholdRef arbeidsforholdRef,
                                                        List<ArbeidsforholdOverstyring> overstyringer) {
        if (arbeidsgiver != null && arbeidsforholdRef != null
            && arbeidsforholdRef.gjelderForSpesifiktArbeidsforhold()) {
            return lagTekstMedArbeidsgiverOgArbeidforholdRef(arbeidsgiver, arbeidsforholdRef, overstyringer);
        }
        if (arbeidsgiver != null) {
            return lagTekstMedArbeidsgiver(arbeidsgiver, overstyringer);
        }
        throw new IllegalStateException("Klarte ikke lage historikkinnslagstekst for arbeidsgiver");
    }

    private String lagArbeidsgiverHistorikkinnslagTekst(Arbeidsgiver arbeidsgiver,
                                                        List<ArbeidsforholdOverstyring> overstyringer) {
        return lagTekstMedArbeidsgiver(arbeidsgiver, overstyringer);
    }

    public String lagHistorikkinnslagTekstForBeregningaktivitet(BeregningAktivitetNøkkel aktivitetNøkkel,
                                                                List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        var arbeidsgiver = aktivitetNøkkel.getArbeidsgiver();
        var arbeidsforholdRef = aktivitetNøkkel.getArbeidsforholdRef();
        if (arbeidsgiver != null) {
            return lagArbeidsgiverHistorikkinnslagTekst(arbeidsgiver, arbeidsforholdRef, arbeidsforholdOverstyringer);
        }
        return aktivitetNøkkel.getOpptjeningAktivitetType().getNavn();
    }

    public String lagHistorikkinnslagTekstForBeregningsgrunnlag(AktivitetStatus aktivitetStatus,
                                                                Optional<Arbeidsgiver> arbeidsgiver,
                                                                Optional<InternArbeidsforholdRef> arbeidsforholdRef,
                                                                List<ArbeidsforholdOverstyring> overstyringer) {
        return arbeidsgiver.map(arbGiv -> arbeidsforholdRef.isPresent() && arbeidsforholdRef.get()
            .gjelderForSpesifiktArbeidsforhold() ? lagArbeidsgiverHistorikkinnslagTekst(arbGiv, arbeidsforholdRef.get(),
            overstyringer) : lagArbeidsgiverHistorikkinnslagTekst(arbGiv, overstyringer))
            .orElse(aktivitetStatus.getNavn());
    }

    private String lagTekstMedArbeidsgiver(Arbeidsgiver arbeidsgiver, List<ArbeidsforholdOverstyring> overstyringer) {
        Objects.requireNonNull(arbeidsgiver, "arbeidsgiver");
        return lagTekstForArbeidsgiver(arbeidsgiver, overstyringer);
    }

    private String lagTekstMedArbeidsgiverOgArbeidforholdRef(Arbeidsgiver arbeidsgiver,
                                                             InternArbeidsforholdRef arbeidsforholdRef,
                                                             List<ArbeidsforholdOverstyring> overstyringer) {
        var sb = new StringBuilder();
        sb.append(lagTekstMedArbeidsgiver(arbeidsgiver, overstyringer));
        sb.append(lagTekstMedArbeidsforholdref(arbeidsforholdRef));
        return sb.toString();
    }

    private String lagTekstMedArbeidsforholdref(InternArbeidsforholdRef arbeidsforholdRef) {
        var referanse = arbeidsforholdRef.getReferanse();
        var sisteFireTegnIRef = referanse.substring(referanse.length() - 4);
        var sb = new StringBuilder();
        sb.append(" ...").append(sisteFireTegnIRef);
        return sb.toString();

    }

    public String lagTekstForArbeidsgiver(Arbeidsgiver arbeidsgiver,
                                          List<ArbeidsforholdOverstyring> arbeidsforholOverstyringer) {
        var opplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
        var sb = new StringBuilder();
        var arbeidsgiverNavn = opplysninger.getNavn();
        if (arbeidsgiver.getErVirksomhet() && Organisasjonstype.erKunstig(arbeidsgiver.getOrgnr())) {
            arbeidsgiverNavn = hentNavnTilManueltArbeidsforhold(arbeidsforholOverstyringer);
        }
        sb.append(arbeidsgiverNavn).append(" (").append(opplysninger.getIdentifikator()).append(")");
        return sb.toString();
    }

    private String hentNavnTilManueltArbeidsforhold(List<ArbeidsforholdOverstyring> arbeidsforholOverstyringer) {
        return arbeidsforholOverstyringer.stream()
            .findFirst()
            .map(ArbeidsforholdOverstyring::getArbeidsgiverNavn)
            .orElseThrow(() -> new IllegalStateException("Utvikler feil: Kaller denne uten overstyring "));
    }
}
