package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.foreldrepenger.domene.iay.modell.BekreftetPermisjon;
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
        Long behandlingId = param.getBehandlingId();

        List<ArbeidsforholdDto> arbeidsforhold = avklarArbeidsforholdDto.getArbeidsforhold();
        List<ArbeidsforholdDto> arbeidsforholdLagtTilAvSaksbehandler = avklarArbeidsforholdDto.getArbeidsforhold().stream()
            .filter(dto -> Boolean.TRUE.equals(dto.getLagtTilAvSaksbehandler()))
            .collect(Collectors.toList());

        List<ArbeidsforholdDto> arbeidsforholdBasertPåInntektsmelding = avklarArbeidsforholdDto.getArbeidsforhold().stream()
            .filter(dto -> Boolean.TRUE.equals(dto.getBasertPaInntektsmelding()))
            .collect(Collectors.toList());


        ArbeidsforholdInformasjonBuilder informasjonBuilder;
        if (!arbeidsforholdLagtTilAvSaksbehandler.isEmpty()) {
            håndterManuelleArbeidsforhold(param);
            informasjonBuilder = arbeidsforholdTjeneste.opprettBuilderFor(behandlingId);
            informasjonBuilder.tilbakestillOverstyringer();
            leggTilArbeidsforholdOppgittAvSaksbehandler(informasjonBuilder, arbeidsforholdLagtTilAvSaksbehandler);
        } else if (!arbeidsforholdBasertPåInntektsmelding.isEmpty()) {
            håndterManuelleArbeidsforhold(param);
            informasjonBuilder = arbeidsforholdTjeneste.opprettBuilderFor(behandlingId);
            informasjonBuilder.tilbakestillOverstyringer();
            leggTilArbeidsforholdBasertPåInntektsmelding(informasjonBuilder, arbeidsforholdBasertPåInntektsmelding);
        } else {
            informasjonBuilder = arbeidsforholdTjeneste.opprettBuilderFor(behandlingId);
            informasjonBuilder.tilbakestillOverstyringer();
            leggPåOverstyringPåOpprinnligeArbeidsforhold(param, informasjonBuilder, arbeidsforhold);
        }

        // krever totrinn hvis saksbehandler har tatt stilling til dette aksjonspunktet
        arbeidsforholdTjeneste.lagre(param.getBehandlingId(), param.getAktørId(), informasjonBuilder);

        return OppdateringResultat.utenTransisjon().medTotrinn().build();
    }

    private void leggTilArbeidsforholdBasertPåInntektsmelding(ArbeidsforholdInformasjonBuilder informasjonBuilder,
                                                              List<ArbeidsforholdDto> arbeidsforholdLagtTilAvSaksbehandler) {
        for (var arbeidsforholdDto : arbeidsforholdLagtTilAvSaksbehandler) {
            ArbeidsforholdHandlingType handlingType = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto);
            ArbeidsforholdOverstyringBuilder overstyrt = leggTilOverstyrt(informasjonBuilder, arbeidsforholdDto, handlingType,
                OrgNummer.erGyldigOrgnr(arbeidsforholdDto.getArbeidsgiverIdentifikator()) ? Arbeidsgiver.virksomhet(arbeidsforholdDto.getArbeidsgiverIdentifikator())
                    : Arbeidsgiver.person(new AktørId(arbeidsforholdDto.getArbeidsgiverIdentifikator())));
            informasjonBuilder.leggTil(overstyrt);
            arbeidsforholdHistorikkinnslagTjeneste.opprettHistorikkinnslag(arbeidsforholdDto, arbeidsforholdDto.getNavn(), Optional.empty());
        }
    }

    private void leggTilArbeidsforholdOppgittAvSaksbehandler(ArbeidsforholdInformasjonBuilder informasjonBuilder,
                                                             List<ArbeidsforholdDto> arbeidsforholdLagtTilAvSaksbehandler) {
        Arbeidsgiver fiktivArbeidsgiver = Arbeidsgiver.virksomhet(FIKTIVT_ORG);
        for (var arbeidsforholdDto : arbeidsforholdLagtTilAvSaksbehandler) {
            ArbeidsforholdHandlingType handlingType = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto);
            ArbeidsforholdOverstyringBuilder overstyrt = leggTilOverstyrt(informasjonBuilder, arbeidsforholdDto, handlingType, fiktivArbeidsgiver);
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
        List<ArbeidsforholdOverstyring> overstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(param.getBehandlingId()).getArbeidsforholdOverstyringer();
        for (ArbeidsforholdDto arbeidsforholdDto : filtrerUtArbeidsforholdSomHarBlittErsattet(arbeidsforhold)) {

            final ArbeidsforholdHandlingType handling = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto);
            final Arbeidsgiver arbeidsgiver = hentArbeidsgiver(arbeidsforholdDto);
            final InternArbeidsforholdRef ref = InternArbeidsforholdRef.ref(arbeidsforholdDto.getArbeidsforholdId());

            ArbeidsforholdOverstyringBuilder overstyringBuilderFor = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref)
                .medBeskrivelse(arbeidsforholdDto.getBegrunnelse())
                .medHandling(handling.equals(ArbeidsforholdHandlingType.SLÅTT_SAMMEN_MED_ANNET)
                    ? ArbeidsforholdHandlingType.BRUK
                    : handling);

            if (arbeidsforholdDto.getBrukPermisjon() != null) {
                BekreftetPermisjon bekreftetPermisjon = UtledBekreftetPermisjon.utled(arbeidsforholdDto);
                overstyringBuilderFor.medBekreftetPermisjon(bekreftetPermisjon);
            }

            if (ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE.equals(handling)) {
                overstyringBuilderFor.leggTilOverstyrtPeriode(arbeidsforholdDto.getFomDato(), arbeidsforholdDto.getOverstyrtTom());
            }

            if (ArbeidsforholdHandlingTypeUtleder.skalErstatteAnnenInntektsmelding(arbeidsforholdDto)) {
                InternArbeidsforholdRef gammelRef = utledArbeidsforholdIdSomSkalErstattes(arbeidsforholdDto.getErstatterArbeidsforholdId(), arbeidsforhold);
                informasjonBuilder.erstattArbeidsforhold(arbeidsgiver, gammelRef, ref);
                final ArbeidsforholdOverstyringBuilder erstattBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, gammelRef);
                erstattBuilder.medNyArbeidsforholdRef(ref);
                erstattBuilder.medHandling(handling);
                informasjonBuilder.leggTil(erstattBuilder);
            }

            informasjonBuilder.leggTil(overstyringBuilderFor);
            arbeidsforholdHistorikkinnslagTjeneste.opprettHistorikkinnslag(param, arbeidsforholdDto, arbeidsgiver, ref, overstyringer);
        }
    }

    private InternArbeidsforholdRef utledArbeidsforholdIdSomSkalErstattes(String erstatterArbeidsforhold, List<ArbeidsforholdDto> arbeidsforhold) {
        final String arbeidsforholdId = arbeidsforhold.stream()
            .filter(af -> af.getId().equalsIgnoreCase(erstatterArbeidsforhold))
            .findAny()
            .map(ArbeidsforholdDto::getArbeidsforholdId)
            .orElseThrow();
        return InternArbeidsforholdRef.ref(arbeidsforholdId);
    }

    private List<ArbeidsforholdDto> filtrerUtArbeidsforholdSomHarBlittErsattet(List<ArbeidsforholdDto> arbeidsforhold) {
        Set<String> filtrertUt = arbeidsforhold.stream()
            .map(ArbeidsforholdDto::getErstatterArbeidsforholdId)
            .collect(Collectors.toSet());
        return arbeidsforhold.stream()
            .filter(a -> !filtrertUt.contains(a.getId()))
            .collect(Collectors.toList());
    }

    private Arbeidsgiver hentArbeidsgiver(ArbeidsforholdDto dto) {
        String identifikator = dto.getArbeidsgiverIdentifikator();
        return OrgNummer.erGyldigOrgnr(identifikator)
            ? Arbeidsgiver.virksomhet(identifikator)
            : Arbeidsgiver.person(new AktørId(identifikator));

    }

    private void håndterManuelleArbeidsforhold(AksjonspunktOppdaterParameter param) {
        Long behandlingId = param.getBehandlingId();
        inntektArbeidYtelseTjeneste.fjernSaksbehandletVersjon(behandlingId);
    }
}
