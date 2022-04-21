package no.nav.foreldrepenger.domene.arbeidInntektsmelding.historikk;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
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
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
public class ArbeidInntektHistorikkinnslagTjeneste {

    private HistorikkTjenesteAdapter historikkAdapter;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    ArbeidInntektHistorikkinnslagTjeneste() {
        // CDI
    }

    @Inject
    ArbeidInntektHistorikkinnslagTjeneste(HistorikkTjenesteAdapter historikkAdapter,
                                          ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }

    public void opprettHistorikkinnslag(BehandlingReferanse behandlingReferanse,
                                        ManglendeOpplysningerVurderingDto vurderingFraSaksbehandler,
                                        InntektArbeidYtelseGrunnlag iayGrunnlag) {
        Arbeidsgiver ag = lagArbeidsgiver(vurderingFraSaksbehandler.getArbeidsgiverIdent());
        InternArbeidsforholdRef internRef = lagInternRef(vurderingFraSaksbehandler.getInternArbeidsforholdRef());
        var eksternRef = finnEksternRef(internRef, ag, iayGrunnlag);
        var opplysninger = arbeidsgiverTjeneste.hent(ag);
        var arbeidsforholdNavn = ArbeidsgiverHistorikkinnslag.lagArbeidsgiverHistorikkinnslagTekst(opplysninger, eksternRef);
        opprettHistorikkinnslagDel(behandlingReferanse, vurderingFraSaksbehandler.getVurdering(), vurderingFraSaksbehandler.getBegrunnelse(), arbeidsforholdNavn);
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
        opprettHistorikkinnslagDel(behandlingReferanse, arbeidsforholdFraSaksbehandler.getVurdering(), arbeidsforholdFraSaksbehandler.getBegrunnelse(), arbeidsforholdNavn);
    }

    private InternArbeidsforholdRef lagInternRef(String internReferanse) {
        return internReferanse == null
            ? InternArbeidsforholdRef.nullRef()
            : InternArbeidsforholdRef.ref(internReferanse);
    }

    private Arbeidsgiver lagArbeidsgiver(String arbeidsgiverIdent) {
        if (OrgNummer.erGyldigOrgnr(arbeidsgiverIdent)) {
            return Arbeidsgiver.virksomhet(arbeidsgiverIdent);
        }
        return Arbeidsgiver.fra(new AktørId(arbeidsgiverIdent));
    }

    private Optional<EksternArbeidsforholdRef> finnEksternRef(InternArbeidsforholdRef internRef, Arbeidsgiver arbeidsgiver, InntektArbeidYtelseGrunnlag iayGrunnlag) {
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

    private void opprettHistorikkinnslagDel(BehandlingReferanse behandlingReferanse,
                                            ArbeidsforholdKomplettVurderingType tilVerdi,
                                            String begrunnelse,
                                            String arbeidsforholdNavn) {
        historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.ARBEIDSFORHOLD, arbeidsforholdNavn, null, tilVerdi);
        historikkAdapter.tekstBuilder().medBegrunnelse(begrunnelse);
        historikkAdapter.tekstBuilder().medSkjermlenke(SkjermlenkeType.FAKTA_OM_ARBEIDSFORHOLD_INNTEKTSMELDING);
        historikkAdapter.opprettHistorikkInnslag(behandlingReferanse.behandlingId(), HistorikkinnslagType.FAKTA_ENDRET);
    }
}
