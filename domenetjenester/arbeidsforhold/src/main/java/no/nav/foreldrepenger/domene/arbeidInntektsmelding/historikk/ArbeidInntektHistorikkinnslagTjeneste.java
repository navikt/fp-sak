package no.nav.foreldrepenger.domene.arbeidInntektsmelding.historikk;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder.fraTilEquals;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ManglendeOpplysningerVurderingDto;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ManueltArbeidsforholdDto;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@ApplicationScoped
public class ArbeidInntektHistorikkinnslagTjeneste {

    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private Historikkinnslag2Repository historikkinnslagRepository;

    ArbeidInntektHistorikkinnslagTjeneste() {
        // CDI
    }

    @Inject
    ArbeidInntektHistorikkinnslagTjeneste(ArbeidsgiverTjeneste arbeidsgiverTjeneste, Historikkinnslag2Repository historikkinnslagRepository) {
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    public void opprettHistorikkinnslag(BehandlingReferanse behandlingReferanse,
                                        ManglendeOpplysningerVurderingDto vurderingFraSaksbehandler,
                                        InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var ag = lagArbeidsgiver(vurderingFraSaksbehandler.getArbeidsgiverIdent());
        var internRef = lagInternRef(vurderingFraSaksbehandler.getInternArbeidsforholdRef());
        var eksternRef = finnEksternRef(internRef, ag, iayGrunnlag);
        var opplysninger = arbeidsgiverTjeneste.hent(ag);
        var arbeidsforholdNavn = ArbeidsgiverHistorikkinnslag.lagArbeidsgiverHistorikkinnslagTekst(opplysninger, eksternRef);
        lagHistorikkinnslag(behandlingReferanse, vurderingFraSaksbehandler.getVurdering(), vurderingFraSaksbehandler.getBegrunnelse(),
            arbeidsforholdNavn);
    }

    public void opprettHistorikkinnslag(BehandlingReferanse behandlingReferanse,
                                        ManueltArbeidsforholdDto arbeidsforholdFraSaksbehandler,
                                        InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var arbeidsgiver = lagArbeidsgiver(arbeidsforholdFraSaksbehandler.getArbeidsgiverIdent());
        var internRef = lagInternRef(arbeidsforholdFraSaksbehandler.getInternArbeidsforholdRef());
        var eksternRef = finnEksternRef(internRef, arbeidsgiver, iayGrunnlag);
        ArbeidsgiverOpplysninger opplysninger;
        if (OrgNummer.erKunstig(arbeidsgiver.getIdentifikator())) {
            var navnFraSaksbehandler = Objects.requireNonNull(arbeidsforholdFraSaksbehandler.getArbeidsgiverNavn(), "arbeidsgivernavn");
            opplysninger = new ArbeidsgiverOpplysninger(arbeidsgiver.getIdentifikator(), navnFraSaksbehandler);
        } else {
            opplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
        }
        var arbeidsforholdNavn = ArbeidsgiverHistorikkinnslag.lagArbeidsgiverHistorikkinnslagTekst(opplysninger, eksternRef);
        lagHistorikkinnslag(behandlingReferanse, arbeidsforholdFraSaksbehandler.getVurdering(), arbeidsforholdFraSaksbehandler.getBegrunnelse(),
            arbeidsforholdNavn);
    }

    private InternArbeidsforholdRef lagInternRef(String internReferanse) {
        return internReferanse == null ? InternArbeidsforholdRef.nullRef() : InternArbeidsforholdRef.ref(internReferanse);
    }

    private Arbeidsgiver lagArbeidsgiver(String arbeidsgiverIdent) {
        if (OrgNummer.erGyldigOrgnr(arbeidsgiverIdent)) {
            return Arbeidsgiver.virksomhet(arbeidsgiverIdent);
        }
        return Arbeidsgiver.fra(new AktørId(arbeidsgiverIdent));
    }

    private Optional<EksternArbeidsforholdRef> finnEksternRef(InternArbeidsforholdRef internRef,
                                                              Arbeidsgiver arbeidsgiver,
                                                              InntektArbeidYtelseGrunnlag iayGrunnlag) {
        if (iayGrunnlag == null) {
            return Optional.empty();
        }
        var referanser = iayGrunnlag.getArbeidsforholdInformasjon()
            .map(ArbeidsforholdInformasjon::getArbeidsforholdReferanser)
            .orElse(Collections.emptyList());
        return referanser.stream()
            .filter(ref -> ref.getArbeidsgiver().equals(arbeidsgiver))
            .filter(ref -> ref.getInternReferanse().gjelderFor(internRef))
            .map(ArbeidsforholdReferanse::getEksternReferanse)
            .findFirst();
    }

    private void lagHistorikkinnslag(BehandlingReferanse behandlingReferanse,
                                     ArbeidsforholdKomplettVurderingType tilVerdi,
                                     String begrunnelse,
                                     String arbeidsforholdNavn) {
        var historikkinnslag = new Historikkinnslag2.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medTittel(SkjermlenkeType.FAKTA_OM_ARBEIDSFORHOLD_INNTEKTSMELDING)
            .medBehandlingId(behandlingReferanse.behandlingId())
            .medFagsakId(behandlingReferanse.fagsakId())
            .addTekstlinje(fraTilEquals("Arbeidsforhold hos " + arbeidsforholdNavn, null, fraArbeidsforholdKomplettVurderingType(tilVerdi)))
            .addTekstlinje(begrunnelse)
            .build();

        historikkinnslagRepository.lagre(historikkinnslag);
    }

    private String fraArbeidsforholdKomplettVurderingType(ArbeidsforholdKomplettVurderingType type) {
        return switch (type) {
            case KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_INNTEKTSMELDING -> "Arbeidsgiver kontaktes";
            case FORTSETT_UTEN_INNTEKTSMELDING -> "Gå videre uten inntektsmelding";
            case KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_ARBEIDSFORHOLD -> "Arbeidsgiver kontaktes";
            case IKKE_OPPRETT_BASERT_PÅ_INNTEKTSMELDING -> "Ikke opprett arbeidsforhold";
            case OPPRETT_BASERT_PÅ_INNTEKTSMELDING -> "Opprettet basert på inntektsmeldingen";
            case MANUELT_OPPRETTET_AV_SAKSBEHANDLER -> "Opprettet av saksbehandler";
            case FJERN_FRA_BEHANDLINGEN -> "Fjernet fra behandlingen";
            case NYTT_ARBEIDSFORHOLD -> "Arbeidsforholdet er ansett som nytt";
            default -> throw new IllegalStateException("Unexpected value ArbeidsforholdKomplettVurderingType: " + type);
        };
    }
}
