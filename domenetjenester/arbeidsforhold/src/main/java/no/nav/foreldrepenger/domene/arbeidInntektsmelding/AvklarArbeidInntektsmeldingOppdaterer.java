package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValg;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.AksjonspunktÅrsak;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftArbeidInntektsmeldingAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarArbeidInntektsmeldingOppdaterer implements AksjonspunktOppdaterer<BekreftArbeidInntektsmeldingAksjonspunktDto> {
    private static final List<ArbeidsforholdHandlingType> HANDLINGER_SOM_LEDER_TIL_TOTRINN = Arrays.asList(ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER,
        ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING);
    private ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    AvklarArbeidInntektsmeldingOppdaterer() {
        // CDI
    }

    @Inject
    AvklarArbeidInntektsmeldingOppdaterer(ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste,
                                          SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                          InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.arbeidsforholdInntektsmeldingMangelTjeneste = arbeidsforholdInntektsmeldingMangelTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(BekreftArbeidInntektsmeldingAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(param.getBehandlingId());
        var alleMangler = arbeidsforholdInntektsmeldingMangelTjeneste.utledAlleManglerPåArbeidsforholdInntektsmelding(param.getRef(), stp);
        var alleSaksbehandlersValg = arbeidsforholdInntektsmeldingMangelTjeneste.hentArbeidsforholdValgForSak(param.getRef());
        validerArbeidsforholdSomManglerInntektsmelding(alleMangler, alleSaksbehandlersValg);

        var alleManueltRegistrerteArbeidsforhold = inntektArbeidYtelseTjeneste.hentGrunnlag(param.getRef().behandlingUuid())
            .getArbeidsforholdInformasjon()
            .map(ArbeidsforholdInformasjon::getOverstyringer)
            .orElse(Collections.emptyList());

        validerInntektsmeldingerSomManglerArbeidsforhold(alleMangler, alleSaksbehandlersValg, alleManueltRegistrerteArbeidsforhold);

        var skalOpprettesTotrinnskontroll = alleManueltRegistrerteArbeidsforhold.stream()
            .anyMatch(os -> HANDLINGER_SOM_LEDER_TIL_TOTRINN.contains(os.getHandling()));

        return OppdateringResultat.utenTransisjon().medTotrinnHvis(skalOpprettesTotrinnskontroll).build();
    }

    private void validerInntektsmeldingerSomManglerArbeidsforhold(List<ArbeidsforholdMangel> alleMangler, List<ArbeidsforholdValg> alleSaksbehandlersValg, List<ArbeidsforholdOverstyring> alleManuelleArbeidsforhold) {
        var eksisterendeMangler = alleMangler.stream()
            .filter(m -> m.årsak().equals(AksjonspunktÅrsak.INNTEKTSMELDING_UTEN_ARBEIDSFORHOLD))
            .toList();

        var aksepterteMangler = alleSaksbehandlersValg.stream()
            .filter(valg -> finnesIListe(eksisterendeMangler, valg))
            .toList();

        var opprettedeArbeidsforholdPåMangel = alleManuelleArbeidsforhold.stream()
            .filter(os -> os.getHandling().equals(ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING))
            .toList();

        // Alle inntektsmeldinger som mangler arbeidsforhold må enten ha et manuelt opprettet arbeidsforhold knyttet til seg,
        // eller så må saksbehandler ha eksplisitt avklart at det ikke skal opprettes arbeidsforhold for inntektsmeldingen.
        var uavklarteMangler = eksisterendeMangler.stream()
            .filter(mangel -> !finnesManueltArbeidsforhold(mangel, opprettedeArbeidsforholdPåMangel)
                && !avklartIkkeRelevant(mangel, aksepterteMangler))
            .toList();


        if (!uavklarteMangler.isEmpty()) {
            throw new IllegalStateException("Det finnes minst en uavklart inntektsmelding uten arbeidsforhold. " +
                "Ugyldig tilstand. Listen over uavklarte mangler: " + uavklarteMangler);
        }

        var arbeidsforholdUlovligOpprettet = opprettedeArbeidsforholdPåMangel.stream()
            .filter(os -> !liggerIMangelListe(os, eksisterendeMangler))
            .toList();

        if (!arbeidsforholdUlovligOpprettet.isEmpty()) {
            var msg = String.format("Det finnes arbeidsforhold basert på inntektsmelding i IAY-aggregat"
                + " som ikke har tilsvarende mangel i aggregatet. Ugyldig tilstand. Alle mangler var %s og ulovlige arbeidsforhold var %s", eksisterendeMangler, arbeidsforholdUlovligOpprettet);
            throw new IllegalStateException(msg);
        }
    }

    private void validerArbeidsforholdSomManglerInntektsmelding(List<ArbeidsforholdMangel> alleMangler, List<ArbeidsforholdValg> alleSaksbehandlersValg) {
        var alleArbeidsforholdSomManglerIM = alleMangler.stream()
            .filter(m -> m.årsak().equals(AksjonspunktÅrsak.MANGLENDE_INNTEKTSMELDING))
            .toList();

        var saksbehandlersValgOmManglendeIM = alleSaksbehandlersValg.stream()
            .filter(valg -> finnesIListe(alleArbeidsforholdSomManglerIM, valg))
            .filter(valg -> valg.getVurdering().equals(ArbeidsforholdKomplettVurderingType.FORTSETT_UTEN_INNTEKTSMELDING))
            .toList();

        if (saksbehandlersValgOmManglendeIM.size() != alleArbeidsforholdSomManglerIM.size()) {
            throw new IllegalStateException("Ikke like mange arbeidsforhold med manglende inntektsmelding som avklarte arbeidsforhold." +
                "Arbeidsforhold uten inntektsmelding: " + alleArbeidsforholdSomManglerIM.size() + ". Valg som er bekreftet: " + alleSaksbehandlersValg.size());
        }

        alleArbeidsforholdSomManglerIM.forEach(mangel -> {
            var arbeidsforholdValg = finnValgSomErGjort(alleSaksbehandlersValg, mangel);
            if (arbeidsforholdValg.isEmpty() || !arbeidsforholdValg.get().getVurdering().equals(ArbeidsforholdKomplettVurderingType.FORTSETT_UTEN_INNTEKTSMELDING)) {
                throw new IllegalStateException("Finnes arbeidsforhold som det ikke er valgt å fortsette uten inntektsmelding på." +
                    " Gjelder arbeidsforhold hos " + mangel.arbeidsgiver() + " med internId " + mangel.ref());
            }
        });
    }

    private boolean avklartIkkeRelevant(ArbeidsforholdMangel mangel, List<ArbeidsforholdValg> saksbehandlersValgOmManglendeArbeidsforhold) {
        return saksbehandlersValgOmManglendeArbeidsforhold.stream()
            .filter(valg -> valg.getVurdering().equals(ArbeidsforholdKomplettVurderingType.IKKE_OPPRETT_BASERT_PÅ_INNTEKTSMELDING))
            .anyMatch(valg -> valg.getArbeidsgiver().equals(mangel.arbeidsgiver()) && valg.getArbeidsforholdRef().gjelderFor(mangel.ref()));
    }

    private boolean finnesManueltArbeidsforhold(ArbeidsforholdMangel mangel, List<ArbeidsforholdOverstyring> overstyringer) {
        return overstyringer.stream().anyMatch(os -> os.getHandling().equals(ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING)
            && os.getArbeidsgiver().equals(mangel.arbeidsgiver())
            && os.getArbeidsforholdRef().gjelderFor(mangel.ref()));
    }

    private boolean finnesIListe(List<ArbeidsforholdMangel> alleArbeidsforholdSomManglerIM, ArbeidsforholdValg valg) {
        return alleArbeidsforholdSomManglerIM.stream()
            .anyMatch(mangel -> mangel.arbeidsgiver().equals(valg.getArbeidsgiver()) && mangel.ref().gjelderFor(valg.getArbeidsforholdRef()));
    }

    private boolean liggerIMangelListe(ArbeidsforholdOverstyring os, List<ArbeidsforholdMangel> inntektsmeldingerSomManglerArbeidsforhold) {
        return inntektsmeldingerSomManglerArbeidsforhold.stream()
            .anyMatch(mangel -> Objects.equals(mangel.arbeidsgiver(), os.getArbeidsgiver())
                && mangel.ref().gjelderFor(os.getArbeidsforholdRef()));
    }

    private Optional<ArbeidsforholdValg> finnValgSomErGjort(List<ArbeidsforholdValg> saksbehandlersValg, ArbeidsforholdMangel mangel) {
        return saksbehandlersValg.stream()
            .filter(valg -> valg.getArbeidsgiver().equals(mangel.arbeidsgiver()) && valg.getArbeidsforholdRef().gjelderFor(mangel.ref()))
            .findFirst();
    }
}
