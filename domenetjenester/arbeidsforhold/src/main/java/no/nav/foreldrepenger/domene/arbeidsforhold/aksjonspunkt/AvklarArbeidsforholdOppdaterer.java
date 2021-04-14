package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarArbeidsforholdDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarArbeidsforholdOppdaterer implements AksjonspunktOppdaterer<AvklarArbeidsforholdDto> {

    private static final String FIKTIVT_ORG = OrgNummer.KUNSTIG_ORG;
    private ArbeidsforholdAdministrasjonTjeneste arbeidsforholdTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private ArbeidsforholdHistorikkinnslagTjeneste arbeidsforholdHistorikkinnslagTjeneste;

    AvklarArbeidsforholdOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarArbeidsforholdOppdaterer(ArbeidsforholdAdministrasjonTjeneste arbeidsforholdTjeneste,
            InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
            ArbeidsforholdHistorikkinnslagTjeneste arbeidsforholdHistorikkinnslagTjeneste) {
        this.arbeidsforholdTjeneste = arbeidsforholdTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.arbeidsforholdHistorikkinnslagTjeneste = arbeidsforholdHistorikkinnslagTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AvklarArbeidsforholdDto avklarArbeidsforholdDto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();

        var arbeidsforhold = avklarArbeidsforholdDto.getArbeidsforhold();
        var arbeidsforholdLagtTilAvSaksbehandler = arbeidsforhold.stream()
                .filter(dto -> Boolean.TRUE.equals(dto.getLagtTilAvSaksbehandler()))
                .collect(Collectors.toList());
        var arbeidsforholdBasertPåInntektsmelding = arbeidsforhold.stream()
                .filter(dto -> Boolean.TRUE.equals(dto.getBasertPaInntektsmelding()))
                .collect(Collectors.toList());
        var opprinneligeArbeidsforhold = arbeidsforhold.stream()
                .filter(dto -> !Boolean.TRUE.equals(dto.getLagtTilAvSaksbehandler()) && !Boolean.TRUE.equals(dto.getBasertPaInntektsmelding()))
                .collect(Collectors.toList());

        var informasjonBuilder = arbeidsforholdTjeneste.opprettBuilderFor(behandlingId).tilbakestillOverstyringer();
        if (!arbeidsforholdLagtTilAvSaksbehandler.isEmpty() || !arbeidsforholdBasertPåInntektsmelding.isEmpty()) {
            håndterManuelleArbeidsforhold(param);
        }
        if (!arbeidsforholdLagtTilAvSaksbehandler.isEmpty()) {
            leggTilArbeidsforholdOppgittAvSaksbehandler(informasjonBuilder, arbeidsforholdLagtTilAvSaksbehandler);
        }
        if (!arbeidsforholdBasertPåInntektsmelding.isEmpty()) {
            leggTilArbeidsforholdBasertPåInntektsmelding(informasjonBuilder, arbeidsforholdBasertPåInntektsmelding);
        }
        leggPåOverstyringPåOpprinnligeArbeidsforhold(param, informasjonBuilder, opprinneligeArbeidsforhold);

        // krever totrinn hvis saksbehandler har tatt stilling til dette aksjonspunktet
        arbeidsforholdTjeneste.lagre(param.getBehandlingId(), param.getAktørId(), informasjonBuilder);

        return OppdateringResultat.utenTransisjon().medTotrinn().build();
    }

    private void leggTilArbeidsforholdBasertPåInntektsmelding(ArbeidsforholdInformasjonBuilder informasjonBuilder,
            List<ArbeidsforholdDto> arbeidsforholdBasertPåInntektsmelding) {
        for (var arbeidsforholdDto : arbeidsforholdBasertPåInntektsmelding) {
            var handlingType = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto);
            var overstyrt = leggTilOverstyrt(informasjonBuilder, arbeidsforholdDto, handlingType,
                    OrgNummer.erGyldigOrgnr(arbeidsforholdDto.getArbeidsgiverIdentifikator())
                            ? Arbeidsgiver.virksomhet(arbeidsforholdDto.getArbeidsgiverIdentifikator())
                            : Arbeidsgiver.person(new AktørId(arbeidsforholdDto.getArbeidsgiverIdentifikator())));
            informasjonBuilder.leggTil(overstyrt);
            arbeidsforholdHistorikkinnslagTjeneste.opprettHistorikkinnslag(arbeidsforholdDto, arbeidsforholdDto.getNavn(), Optional.empty());
        }
    }

    private void leggTilArbeidsforholdOppgittAvSaksbehandler(ArbeidsforholdInformasjonBuilder informasjonBuilder,
            List<ArbeidsforholdDto> arbeidsforholdLagtTilAvSaksbehandler) {
        var fiktivArbeidsgiver = Arbeidsgiver.virksomhet(FIKTIVT_ORG);
        for (var arbeidsforholdDto : arbeidsforholdLagtTilAvSaksbehandler) {
            var handlingType = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto);
            var overstyrt = leggTilOverstyrt(informasjonBuilder, arbeidsforholdDto, handlingType, fiktivArbeidsgiver);
            informasjonBuilder.leggTil(overstyrt);
            arbeidsforholdHistorikkinnslagTjeneste.opprettHistorikkinnslag(arbeidsforholdDto, arbeidsforholdDto.getNavn(), Optional.empty());
        }
    }

    private ArbeidsforholdOverstyringBuilder leggTilOverstyrt(ArbeidsforholdInformasjonBuilder informasjonBuilder,
            ArbeidsforholdDto arbeidsforholdDto,
            ArbeidsforholdHandlingType basertPåInntektsmelding,
            Arbeidsgiver arbeidsgiver) {

        var overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver,
                InternArbeidsforholdRef.ref(arbeidsforholdDto.getArbeidsforholdId()));
        var stillingsprosent = Objects.requireNonNull(arbeidsforholdDto.getStillingsprosent(), "stillingsprosent");
        return overstyringBuilder.medHandling(basertPåInntektsmelding)
                .medAngittArbeidsgiverNavn(arbeidsforholdDto.getNavn())
                .medBeskrivelse(arbeidsforholdDto.getBegrunnelse())
                .medAngittStillingsprosent(new Stillingsprosent(stillingsprosent))
                .leggTilOverstyrtPeriode(arbeidsforholdDto.getFomDato(),
                        arbeidsforholdDto.getTomDato() == null ? TIDENES_ENDE : arbeidsforholdDto.getTomDato());
    }

    private void leggPåOverstyringPåOpprinnligeArbeidsforhold(AksjonspunktOppdaterParameter param,
            ArbeidsforholdInformasjonBuilder informasjonBuilder,
            List<ArbeidsforholdDto> arbeidsforhold) {
        var overstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(param.getBehandlingId())
                .getArbeidsforholdOverstyringer();
        var aktuelle = filtrerUtArbeidsforholdSomHarBlittErsattet(arbeidsforhold);
        for (var arbeidsforholdDto : aktuelle) {

            final var handling = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto);
            final var arbeidsgiver = hentArbeidsgiver(arbeidsforholdDto);
            final var ref = InternArbeidsforholdRef.ref(arbeidsforholdDto.getArbeidsforholdId());

            var overstyringBuilderFor = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref)
                    .medBeskrivelse(arbeidsforholdDto.getBegrunnelse())
                    .medHandling(handling.equals(ArbeidsforholdHandlingType.SLÅTT_SAMMEN_MED_ANNET)
                            ? ArbeidsforholdHandlingType.BRUK
                            : handling);

            if (arbeidsforholdDto.getBrukPermisjon() != null) {
                var bekreftetPermisjon = UtledBekreftetPermisjon.utled(arbeidsforholdDto);
                overstyringBuilderFor.medBekreftetPermisjon(bekreftetPermisjon);
            }

            if (ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE.equals(handling)) {
                overstyringBuilderFor.leggTilOverstyrtPeriode(arbeidsforholdDto.getFomDato(), arbeidsforholdDto.getOverstyrtTom());
            }

            if (ArbeidsforholdHandlingTypeUtleder.skalErstatteAnnenInntektsmelding(arbeidsforholdDto)) {
                var gammelRef = utledArbeidsforholdIdSomSkalErstattes(arbeidsforholdDto.getErstatterArbeidsforholdId(),
                        arbeidsforhold);
                informasjonBuilder.erstattArbeidsforhold(arbeidsgiver, gammelRef, ref);
                final var erstattBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, gammelRef);
                erstattBuilder.medNyArbeidsforholdRef(ref);
                erstattBuilder.medHandling(handling);
                informasjonBuilder.leggTil(erstattBuilder);
            }

            informasjonBuilder.leggTil(overstyringBuilderFor);
            arbeidsforholdHistorikkinnslagTjeneste.opprettHistorikkinnslag(param, arbeidsforholdDto, arbeidsgiver, ref, overstyringer);
        }
    }

    private InternArbeidsforholdRef utledArbeidsforholdIdSomSkalErstattes(String erstatterArbeidsforhold, List<ArbeidsforholdDto> arbeidsforhold) {
        final var arbeidsforholdId = arbeidsforhold.stream()
                .filter(af -> af.getId().equalsIgnoreCase(erstatterArbeidsforhold))
                .findAny()
                .map(ArbeidsforholdDto::getArbeidsforholdId)
                .orElseThrow();
        return InternArbeidsforholdRef.ref(arbeidsforholdId);
    }

    private List<ArbeidsforholdDto> filtrerUtArbeidsforholdSomHarBlittErsattet(List<ArbeidsforholdDto> arbeidsforhold) {
        var filtrertUt = arbeidsforhold.stream()
                .map(ArbeidsforholdDto::getErstatterArbeidsforholdId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return arbeidsforhold.stream()
                .filter(a -> !filtrertUt.contains(a.getId()))
                .collect(Collectors.toList());
    }

    private Arbeidsgiver hentArbeidsgiver(ArbeidsforholdDto dto) {
        var identifikator = dto.getArbeidsgiverIdentifikator();
        return OrgNummer.erGyldigOrgnr(identifikator)
                ? Arbeidsgiver.virksomhet(identifikator)
                : Arbeidsgiver.person(new AktørId(identifikator));

    }

    private void håndterManuelleArbeidsforhold(AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        inntektArbeidYtelseTjeneste.fjernSaksbehandletVersjon(behandlingId);
    }
}
