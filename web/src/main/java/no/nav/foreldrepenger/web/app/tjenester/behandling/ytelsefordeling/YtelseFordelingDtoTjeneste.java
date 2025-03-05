package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.Annenpart;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class YtelseFordelingDtoTjeneste {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private UføretrygdRepository uføretrygdRepository;
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private BehandlingRepository behandlingRepository;
    private UttakInputTjeneste uttakInputTjeneste;

    YtelseFordelingDtoTjeneste() {
        //CDI
    }

    @Inject
    public YtelseFordelingDtoTjeneste(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                      UføretrygdRepository uføretrygdRepository,
                                      ForeldrepengerUttakTjeneste uttakTjeneste,
                                      PersonopplysningTjeneste personopplysningTjeneste,
                                      BehandlingRepository behandlingRepository,
                                      UttakInputTjeneste uttakInputTjeneste) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.uføretrygdRepository = uføretrygdRepository;
        this.uttakTjeneste = uttakTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.uttakInputTjeneste = uttakInputTjeneste;
    }

    public Optional<YtelseFordelingDto> mapFra(Behandling behandling) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandling.getId());
        var dtoBuilder = new YtelseFordelingDto.Builder();
        ytelseFordelingAggregat.ifPresent(yfa -> {
            dtoBuilder.medBekreftetAleneomsorg(yfa.getAleneomsorgAvklaring());
            dtoBuilder.medOverstyrtOmsorg(yfa.getOverstyrtOmsorg());
            dtoBuilder.medFørsteUttaksdato(finnFørsteUttaksdato(behandling));
            dtoBuilder.medØnskerJustertVedFødsel(yfa.getGjeldendeFordeling().ønskerJustertVedFødsel());
            dtoBuilder.medRettigheterAnnenforelder(lagAnnenforelderRettDto(behandling, yfa));
        });
        return Optional.of(dtoBuilder.build());
    }

    private RettigheterAnnenforelderDto lagAnnenforelderRettDto(Behandling behandling, YtelseFordelingAggregat yfa) {
        var uføregrunnlag = uføretrygdRepository.hentGrunnlag(behandling.getId());
        var avklareUføretrygd =
            yfa.getMorUføretrygdAvklaring() == null && uføregrunnlag.filter(UføretrygdGrunnlagEntitet::uavklartAnnenForelderMottarUføretrygd)
                .isPresent();
        var avklareRettEØS = yfa.getAnnenForelderRettEØSAvklaring() == null && yfa.oppgittAnnenForelderTilknytningEØS();
        return new RettigheterAnnenforelderDto(yfa.getAnnenForelderRettAvklaring(), yfa.getAnnenForelderRettEØSAvklaring(), avklareRettEØS,
            yfa.getMorUføretrygdAvklaring(), avklareUføretrygd);
    }

    public LocalDate finnFørsteUttaksdato(Behandling behandling) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandling.getId());
        var førsteUttaksdato = ytelseFordelingAggregat.getAvklarteDatoer().map(AvklarteUttakDatoerEntitet::getFørsteUttaksdato);
        return førsteUttaksdato.orElseGet(
            () -> behandling.erRevurdering() ? finnFørsteUttaksdatoRevurdering(behandling) : finnFørsteUttaksdatoFørstegangsbehandling(behandling));
    }

    private LocalDate finnFørsteUttaksdatoFørstegangsbehandling(Behandling behandling) {
        return ytelseFordelingTjeneste.hentAggregat(behandling.getId()).getGjeldendeFordeling().finnFørsteUttaksdato().orElseThrow();
    }

    private LocalDate finnFørsteUttaksdatoRevurdering(Behandling behandling) {
        var originalBehandling = behandling.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Original behandling mangler på revurdering - skal ikke skje"));
        var uttakOriginal = uttakTjeneste.hentHvisEksisterer(originalBehandling);
        var førsteUttakOriginal = uttakOriginal.flatMap(ForeldrepengerUttak::finnFørsteUttaksdatoHvisFinnes);
        var førsteUttaksdatoTidligereBehandling = førsteUttakOriginal.orElse(Tid.TIDENES_ENDE);

        var førsteUttaksdatoSøkt = ytelseFordelingTjeneste.hentAggregat(behandling.getId()).getOppgittFordeling().finnFørsteUttaksdato();

        return førsteUttaksdatoSøkt.filter(søktFom -> søktFom.isBefore(førsteUttaksdatoTidligereBehandling))
            .orElse(førsteUttaksdatoTidligereBehandling);
    }


    Optional<OmsorgOgRettDto> mapFra(UUID behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid);
        if (!behandling.getFagsakYtelseType().equals(FagsakYtelseType.FORELDREPENGER)) {
            return Optional.empty();
        }
        var behandlingId = behandling.getId();
        var aktørId = behandling.getAktørId();
        var yfaOpt = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandlingId);
        if (yfaOpt.isEmpty()) {
            return Optional.empty();
        }
        var poaOpt = personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(behandlingId, aktørId);
        if (poaOpt.isEmpty()) {
            return Optional.empty();
        }
        var ytelseFordelingAggregat = yfaOpt.get();
        var personopplysningerAggregat = poaOpt.get();

        var oppgittAnnenpart = personopplysningerAggregat.getOppgittAnnenPart();
        var oppgittAleneomsorg = Boolean.TRUE.equals(ytelseFordelingAggregat.getOppgittRettighet().getHarAleneomsorgForBarnet());
        var registerdata = opprettRegisterdata(behandlingId, oppgittAleneomsorg);
        var manuellBehandlingResultat = opprettManuellBehandlingResultat(ytelseFordelingAggregat);

        var søknad = mapSøknad(oppgittAnnenpart.orElse(null), ytelseFordelingAggregat.getOppgittRettighet());
        return Optional.of(
            new OmsorgOgRettDto(søknad, registerdata.orElse(null), manuellBehandlingResultat.orElse(null), behandling.getRelasjonsRolleType()));
    }

    private Optional<OmsorgOgRettDto.RegisterData> opprettRegisterdata(Long behandlingId, boolean oppgittAleneomsorg) {
        var ytelsespesifiktGrunnlag = hentForeldrepengerGrunnlag(behandlingId);
        var harAnnenpartForeldrepenger = oppgittAleneomsorg ? null : ytelsespesifiktGrunnlag.getAnnenpart()
            .map(Annenpart::gjeldendeVedtakBehandlingId)
            .flatMap(uttakTjeneste::hentHvisEksisterer)
            .filter(ForeldrepengerUttak::harUtbetaling)
            .isPresent();
        var harAnnenpartEngangsstønad = oppgittAleneomsorg ? null : ytelsespesifiktGrunnlag.isOppgittAnnenForelderHarEngangsstønadForSammeBarn();
        var annenForelderMottarUføretrygd = uføretrygdRepository.hentGrunnlag(behandlingId)
            .map(UføretrygdGrunnlagEntitet::annenForelderMottarUføretrygd)
            .orElse(null);
        if (harAnnenpartForeldrepenger == null && harAnnenpartEngangsstønad == null && annenForelderMottarUføretrygd == null) {
            return Optional.empty();
        }
        return Optional.of(new OmsorgOgRettDto.RegisterData(annenForelderMottarUføretrygd, harAnnenpartForeldrepenger, harAnnenpartEngangsstønad));
    }

    private static Optional<OmsorgOgRettDto.ManuellBehandlingResultat> opprettManuellBehandlingResultat(YtelseFordelingAggregat ytelseFordelingAggregat) {
        return ytelseFordelingAggregat.getOverstyrtRettighet().map(or -> {
            var søkerHarAleneomsorg = or.getHarAleneomsorgForBarnet();
            var annenpartRettighet = Objects.equals(søkerHarAleneomsorg, Boolean.TRUE) ? null : new OmsorgOgRettDto.Rettighet(
                or.getHarAnnenForeldreRett(), or.getAnnenForelderOppholdEØS(), or.getAnnenForelderRettEØSNullable(), or.getMorMottarUføretrygd());
            return new OmsorgOgRettDto.ManuellBehandlingResultat(søkerHarAleneomsorg, annenpartRettighet);
        });
    }

    private ForeldrepengerGrunnlag hentForeldrepengerGrunnlag(Long behandlingId) {
        var uttakInput = uttakInputTjeneste.lagInput(behandlingId);
        return uttakInput.getYtelsespesifiktGrunnlag();
    }

    private static OmsorgOgRettDto.Søknad mapSøknad(OppgittAnnenPartEntitet ap, OppgittRettighetEntitet oppgittRettighet) {
        var ident = utledAnnenpartIdent(ap);

        var harAleneomsorg = oppgittRettighet.getHarAleneomsorgForBarnet();
        var rettighet = Objects.equals(harAleneomsorg, Boolean.TRUE) ? null : ikkeAleneomsorgRettighet(oppgittRettighet);
        var utenlandskFnrLand = Optional.ofNullable(ap).map(OppgittAnnenPartEntitet::getUtenlandskFnrLand).orElse(null);
        return new OmsorgOgRettDto.Søknad(harAleneomsorg, ident.orElse(null), utenlandskFnrLand, rettighet);
    }

    private static OmsorgOgRettDto.Rettighet ikkeAleneomsorgRettighet(OppgittRettighetEntitet oppgittRettighet) {
        var harAnnenForeldreRett = oppgittRettighet.getHarAnnenForeldreRett();
        if (harAnnenForeldreRett) {
            return new OmsorgOgRettDto.Rettighet(true, null, null, null);
        }
        var annenForelderRettEØSNullable = Boolean.TRUE.equals(
            oppgittRettighet.getAnnenForelderOppholdEØS()) ? oppgittRettighet.getAnnenForelderRettEØSNullable() : null; //Bruker får ikke spørsmål om rett/arbeid eøs hvis man ikke har oppgitt at annen part har opphold i eøs
        return new OmsorgOgRettDto.Rettighet(false, oppgittRettighet.getAnnenForelderOppholdEØS(), annenForelderRettEØSNullable,
            oppgittRettighet.getMorMottarUføretrygd());
    }

    private static Optional<String> utledAnnenpartIdent(OppgittAnnenPartEntitet ap) {
        return Optional.ofNullable(ap).map(OppgittAnnenPartEntitet::getUtenlandskPersonident);
    }
}
