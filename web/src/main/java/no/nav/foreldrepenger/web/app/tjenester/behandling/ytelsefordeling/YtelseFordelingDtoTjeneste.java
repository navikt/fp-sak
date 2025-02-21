package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.Annenpart;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonadresseDto;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class YtelseFordelingDtoTjeneste {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private DekningsgradTjeneste dekningsgradTjeneste;
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
                                      DekningsgradTjeneste dekningsgradTjeneste,
                                      UføretrygdRepository uføretrygdRepository,
                                      ForeldrepengerUttakTjeneste uttakTjeneste,
                                      PersonopplysningTjeneste personopplysningTjeneste,
                                      BehandlingRepository behandlingRepository,
                                      UttakInputTjeneste uttakInputTjeneste) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.dekningsgradTjeneste = dekningsgradTjeneste;
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
            yfa.getAvklarteDatoer().ifPresent(avklarteUttakDatoer -> dtoBuilder.medEndringsdato(avklarteUttakDatoer.getGjeldendeEndringsdato()));
            dtoBuilder.medFørsteUttaksdato(finnFørsteUttaksdato(behandling));
            dtoBuilder.medØnskerJustertVedFødsel(yfa.getGjeldendeFordeling().ønskerJustertVedFødsel());
            dtoBuilder.medRettigheterAnnenforelder(lagAnnenforelderRettDto(behandling, yfa));
        });
        var fagsdekningsgradkRelasjon = dekningsgradTjeneste.finnGjeldendeDekningsgradHvisEksisterer(BehandlingReferanse.fra(behandling));
        fagsdekningsgradkRelasjon.ifPresent(d -> dtoBuilder.medGjeldendeDekningsgrad(d.getVerdi()));
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
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandlingId);
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(behandlingId, behandling.getAktørId());
        if (personopplysningerAggregat.isEmpty()) {
            return Optional.empty();
        }
        var uføretrygdGrunnlagEntitet = uføretrygdRepository.hentGrunnlag(behandlingId);

        var ytelsespesifiktGrunnlag = hentForeldrepengerGrunnlag(behandlingId);

        return mapTilDto(behandling.getAktørId(), ytelseFordelingAggregat, personopplysningerAggregat.get(), uføretrygdGrunnlagEntitet,
            ytelsespesifiktGrunnlag,
            ytelsespesifiktGrunnlag.getAnnenpart().map(Annenpart::gjeldendeVedtakBehandlingId).flatMap(b -> uttakTjeneste.hentHvisEksisterer(b)));
    }

    private static Optional<OmsorgOgRettDto> mapTilDto(AktørId aktørId,
                                                       YtelseFordelingAggregat ytelseFordelingAggregat,
                                                       PersonopplysningerAggregat personopplysningerAggregat,
                                                       Optional<UføretrygdGrunnlagEntitet> uføretrygdGrunnlagEntitet,
                                                       ForeldrepengerGrunnlag ytelsespesifiktGrunnlag,
                                                       Optional<ForeldrepengerUttak> annenpartsUttak) {
        var oppgittAleneomsorg = ytelseFordelingAggregat.getOppgittRettighet().getHarAleneomsorgForBarnet();
        var harAnnenpartForeldrepenger = oppgittAleneomsorg ? null : annenpartsUttak.filter(ForeldrepengerUttak::harUtbetaling).isPresent();
        var harAnnenpartEngangsstønad = oppgittAleneomsorg ? null : ytelsespesifiktGrunnlag.isOppgittAnnenForelderHarEngangsstønadForSammeBarn();
        var sivilstand = personopplysningerAggregat.getSøker().getSivilstand();

        var oppgittAnnenpart = personopplysningerAggregat.getOppgittAnnenPart()
            .flatMap(ap -> mapAnnenpart(ap, ytelseFordelingAggregat.getOppgittRettighet()));

        var manuellBehandlingResultat = ytelseFordelingAggregat.getOverstyrtRettighet()
            .map(or -> new OmsorgOgRettDto.ManuellBehandlingResultat(or.getHarAleneomsorgForBarnet(), or.getHarAnnenForeldreRett(),
                or.getAnnenForelderOppholdEØS(), or.getAnnenForelderRettEØSNullable(), or.getMorMottarUføretrygd()));
        var adresserSøker = adresserForPerson(personopplysningerAggregat, aktørId);
        var adresserAnnenpart = personopplysningerAggregat.getOppgittAnnenPart()
            .map(a -> adresserForPerson(personopplysningerAggregat, a.getAktørId()))
            .orElse(Set.of());
        var barnasAdresser = personopplysningerAggregat.getBarna()
            .stream()
            .flatMap(b -> adresserForPerson(personopplysningerAggregat, b.getAktørId()).stream())
            .collect(Collectors.toSet());
        var registerdata = new OmsorgOgRettDto.RegisterData(adresserSøker, adresserAnnenpart, barnasAdresser, sivilstand,
            uføretrygdGrunnlagEntitet.map(UføretrygdGrunnlagEntitet::annenForelderMottarUføretrygd).orElse(null), harAnnenpartForeldrepenger,
            harAnnenpartEngangsstønad);

        return Optional.of(new OmsorgOgRettDto(oppgittAnnenpart.orElse(null), registerdata, manuellBehandlingResultat.orElse(null)));
    }

    private static Set<PersonadresseDto> adresserForPerson(PersonopplysningerAggregat personopplysningerAggregat, AktørId aktørId) {
        return personopplysningerAggregat.getAdresserFor(aktørId,
                SimpleLocalDateInterval.fraOgMedTomNotNull(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE))
            .stream()
            .map(PersonadresseDto::tilDto)
            .collect(Collectors.toSet());
    }

    private ForeldrepengerGrunnlag hentForeldrepengerGrunnlag(Long behandlingId) {
        var uttakInput = uttakInputTjeneste.lagInput(behandlingId);
        return uttakInput.getYtelsespesifiktGrunnlag();
    }

    private static Optional<OmsorgOgRettDto.Søknad> mapAnnenpart(OppgittAnnenPartEntitet ap, OppgittRettighetEntitet oppgittRettighet) {
        var ident = utledAnnenpartIdent(ap);
        return ident.map(s -> {
            var harAleneomsorg = oppgittRettighet.getHarAleneomsorgForBarnet();
            var rettighet = harAleneomsorg ? null : new OmsorgOgRettDto.Søknad.Rettighet(oppgittRettighet.getHarAnnenForeldreRett(),
                oppgittRettighet.getAnnenForelderOppholdEØS(), oppgittRettighet.getAnnenForelderRettEØS(), oppgittRettighet.getMorMottarUføretrygd());
            return new OmsorgOgRettDto.Søknad(harAleneomsorg, ap.getNavn(), s, ap.getUtenlandskFnrLand(), rettighet);
        });
    }

    private static Optional<String> utledAnnenpartIdent(OppgittAnnenPartEntitet ap) {
        if (ap.getAktørId() == null && ap.getUtenlandskPersonident() == null) {
            return Optional.empty();
        }
        return Optional.of(ap.getUtenlandskPersonident() == null ? ap.getAktørId().getId() : ap.getUtenlandskPersonident());
    }
}
