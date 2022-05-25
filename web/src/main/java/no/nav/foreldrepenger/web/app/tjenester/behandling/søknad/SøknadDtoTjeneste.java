package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.familiehendelse.rest.SøknadType;
import no.nav.foreldrepenger.kompletthet.KompletthetsjekkerProvider;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class SøknadDtoTjeneste {

    private BehandlingRepositoryProvider repositoryProvider;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private KompletthetsjekkerProvider kompletthetsjekkerProvider;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private YtelsesFordelingRepository ytelsesfordelingRepository;
    private MedlemTjeneste medlemTjeneste;
    private PersonopplysningRepository personopplysningRepository;

    protected SøknadDtoTjeneste() {
        // for CDI proxy
    }

    @Inject
    public SøknadDtoTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                 SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                 KompletthetsjekkerProvider kompletthetsjekkerProvider,
                                 YtelsesFordelingRepository ytelsesfordelingRepository,
                                 PersonopplysningRepository personopplysningRepository,
                                 MedlemTjeneste medlemTjeneste) {
        this.repositoryProvider = repositoryProvider;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.kompletthetsjekkerProvider = kompletthetsjekkerProvider;
        this.ytelsesfordelingRepository = ytelsesfordelingRepository;
        this.personopplysningRepository = personopplysningRepository;
        this.medlemTjeneste = medlemTjeneste;
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
    }

    public Optional<SoknadDto> mapFra(Behandling behandling) {
        var søknadOpt = repositoryProvider.getSøknadRepository().hentSøknadHvisEksisterer(behandling.getId());
        if (søknadOpt.isPresent()) {
            var søknad = søknadOpt.get();
            var ref = BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
            var fhGrunnlag = familieHendelseRepository.hentAggregat(behandling.getId());
            if (fhGrunnlag.getSøknadVersjon().getGjelderFødsel()) {
                return lagSoknadFodselDto(søknad, fhGrunnlag.getSøknadVersjon(), ref);
            }
            if (fhGrunnlag.getSøknadVersjon().getGjelderAdopsjon()) {
                return lagSoknadAdopsjonDto(søknad, fhGrunnlag.getSøknadVersjon(), ref);
            }
        }
        return Optional.empty();
    }

    public Optional<SoknadBackendDto> mapForBackend(Behandling behandling) {
        return repositoryProvider.getSøknadRepository().hentSøknadHvisEksisterer(behandling.getId())
            .map(søknad -> getBackendDto(behandling, søknad));
    }

    private SoknadBackendDto getBackendDto(Behandling behandling, SøknadEntitet søknad) {
        var familieHendelse = familieHendelseRepository.hentAggregat(behandling.getId()).getSøknadVersjon();

        var soknadBackendDto = new SoknadBackendDto();
        soknadBackendDto.setMottattDato(søknad.getMottattDato());
        soknadBackendDto.setSoknadsdato(søknad.getSøknadsdato());
        soknadBackendDto.setSpraakkode(søknad.getSpråkkode());
        soknadBackendDto.setSoknadType(familieHendelse.getGjelderFødsel() ? SøknadType.FØDSEL : SøknadType.ADOPSJON);

        ytelsesfordelingRepository.hentAggregatHvisEksisterer(behandling.getId())
            .ifPresent(of -> soknadBackendDto.setOppgittRettighet(OppgittRettighetDto.mapFra(of.getOppgittRettighet())));
        ytelsesfordelingRepository.hentAggregatHvisEksisterer(behandling.getId())
            .map(YtelseFordelingAggregat::getOppgittRettighet)
            .map(OppgittRettighetEntitet::getHarAleneomsorgForBarnet)
            .ifPresent(soknadBackendDto::setOppgittAleneomsorg);

        return soknadBackendDto;
    }

    private Optional<SoknadDto> lagSoknadFodselDto(SøknadEntitet søknad, FamilieHendelseEntitet familieHendelse, BehandlingReferanse ref) {
        var behandlingId = ref.behandlingId();

        var soknadFodselDto = new SoknadFodselDto();
        var fødselsdatoer = familieHendelse.getBarna().stream()
            .collect(Collectors.toMap(UidentifisertBarn::getBarnNummer, UidentifisertBarn::getFødselsdato));
        mapFellesSoknadDtoFelter(søknad, soknadFodselDto);
        soknadFodselDto.setSoknadType(SøknadType.FØDSEL);
        soknadFodselDto.setUtstedtdato(familieHendelse.getTerminbekreftelse().map(TerminbekreftelseEntitet::getUtstedtdato).orElse(null));
        soknadFodselDto.setTermindato(familieHendelse.getTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato).orElse(null));
        soknadFodselDto.setAntallBarn(familieHendelse.getAntallBarn());
        soknadFodselDto.setBegrunnelseForSenInnsending(søknad.getBegrunnelseForSenInnsending());
        soknadFodselDto.setFarSokerType(søknad.getFarSøkerType());

        personopplysningRepository.hentOppgittAnnenPartHvisEksisterer(behandlingId).map(OppgittAnnenPartEntitet::getNavn).ifPresent(soknadFodselDto::setAnnenPartNavn);

        ytelsesfordelingRepository.hentAggregatHvisEksisterer(behandlingId).ifPresent(of -> {
            soknadFodselDto.setOppgittRettighet(OppgittRettighetDto.mapFra(of.getOppgittRettighet()));
            soknadFodselDto.setOppgittFordeling(OppgittFordelingDto.mapFra(of.getOppgittFordeling(), hentOppgittStartdatoForPermisjon(behandlingId, søknad.getRelasjonsRolleType())));

        });

        medlemTjeneste.hentMedlemskap(behandlingId).ifPresent(ma -> {
            soknadFodselDto.setOppgittTilknytning(OppgittTilknytningDto.mapFra(ma.getOppgittTilknytning().orElse(null)));
        });

        soknadFodselDto.setManglendeVedlegg(genererManglendeVedlegg(ref));
        soknadFodselDto.setDekningsgrad(hentDekningsgrad(ref).orElse(null));
        soknadFodselDto.setFodselsdatoer(fødselsdatoer);

        return Optional.of(soknadFodselDto);
    }

    private void mapFellesSoknadDtoFelter(SøknadEntitet søknad, SoknadDto soknadDto) {
        soknadDto.setMottattDato(søknad.getMottattDato());
        soknadDto.setSoknadsdato(søknad.getSøknadsdato());
        soknadDto.setTilleggsopplysninger(søknad.getTilleggsopplysninger());
        soknadDto.setSpraakkode(søknad.getSpråkkode());
    }

    private List<ManglendeVedleggDto> genererManglendeVedlegg(BehandlingReferanse ref) {
        var kompletthetsjekker = kompletthetsjekkerProvider.finnKompletthetsjekkerFor(ref.fagsakYtelseType(), ref.behandlingType());
        final var alleManglendeVedlegg = kompletthetsjekker.utledAlleManglendeVedleggForForsendelse(ref);
        final var vedleggSomIkkeKommer = kompletthetsjekker.utledAlleManglendeVedleggSomIkkeKommer(ref);

        // Fjerner slik at det ikke blir dobbelt opp, og for å markere korrekt hvilke som ikke vil komme
        alleManglendeVedlegg.removeIf(e -> vedleggSomIkkeKommer.stream().anyMatch(it -> it.getArbeidsgiver().equals(e.getArbeidsgiver())));
        alleManglendeVedlegg.addAll(vedleggSomIkkeKommer);

        return alleManglendeVedlegg.stream().map(this::mapTilManglendeVedleggDto).collect(Collectors.toList());
    }

    private ManglendeVedleggDto mapTilManglendeVedleggDto(ManglendeVedlegg mv) {
        final var dto = new ManglendeVedleggDto();
        dto.setDokumentType(mv.getDokumentType());
        if (mv.getDokumentType().equals(DokumentTypeId.INNTEKTSMELDING)) {
            dto.setArbeidsgiverReferanse(mv.getArbeidsgiver());
            dto.setBrukerHarSagtAtIkkeKommer(mv.getBrukerHarSagtAtIkkeKommer());
        }
        return dto;
    }

    private Optional<SoknadDto> lagSoknadAdopsjonDto(SøknadEntitet søknad, FamilieHendelseEntitet familieHendelse, BehandlingReferanse ref) {
        var behandlingId = ref.behandlingId();
        var fødselsdatoer = familieHendelse.getBarna().stream()
            .collect(Collectors.toMap(UidentifisertBarn::getBarnNummer, UidentifisertBarn::getFødselsdato));
        var soknadAdopsjonDto = new SoknadAdopsjonDto();
        mapFellesSoknadDtoFelter(søknad, soknadAdopsjonDto);
        soknadAdopsjonDto.setSoknadType(SøknadType.ADOPSJON);
        soknadAdopsjonDto.setOmsorgsovertakelseDato(familieHendelse.getAdopsjon().map(AdopsjonEntitet::getOmsorgsovertakelseDato).orElse(null));
        soknadAdopsjonDto.setBarnetsAnkomstTilNorgeDato(familieHendelse.getAdopsjon().map(AdopsjonEntitet::getAnkomstNorgeDato).orElse(null));
        soknadAdopsjonDto.setFarSokerType(søknad.getFarSøkerType());
        soknadAdopsjonDto.setAdopsjonFodelsedatoer(fødselsdatoer);
        soknadAdopsjonDto.setAntallBarn(familieHendelse.getAntallBarn());
        soknadAdopsjonDto.setBegrunnelseForSenInnsending(søknad.getBegrunnelseForSenInnsending());

        personopplysningRepository.hentOppgittAnnenPartHvisEksisterer(behandlingId).map(OppgittAnnenPartEntitet::getNavn).ifPresent(soknadAdopsjonDto::setAnnenPartNavn);

        ytelsesfordelingRepository.hentAggregatHvisEksisterer(behandlingId).ifPresent(of -> {
            soknadAdopsjonDto.setOppgittRettighet(OppgittRettighetDto.mapFra(of.getOppgittRettighet()));
            soknadAdopsjonDto.setOppgittFordeling(OppgittFordelingDto.mapFra(of.getOppgittFordeling(), hentOppgittStartdatoForPermisjon(ref.behandlingId(), null)));
        });

        medlemTjeneste.hentMedlemskap(behandlingId).ifPresent(ma -> {
            soknadAdopsjonDto.setOppgittTilknytning(OppgittTilknytningDto.mapFra(ma.getOppgittTilknytning().orElse(null)));
        });

        soknadAdopsjonDto.setDekningsgrad(hentDekningsgrad(ref).orElse(null));
        soknadAdopsjonDto.setManglendeVedlegg(genererManglendeVedlegg(ref));
        return Optional.of(soknadAdopsjonDto);
    }

    private Optional<LocalDate> hentOppgittStartdatoForPermisjon(Long behandlingId, RelasjonsRolleType rolleType) {
        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);

        var oppgittStartdato = getFørsteUttaksdagHvisOppgitt(skjæringstidspunkter)
            .or(() -> skjæringstidspunkter.getSkjæringstidspunktHvisUtledet());
        if (RelasjonsRolleType.MORA.equals(rolleType)) {
            var evFødselFørOppgittStartdato = familieHendelseRepository.hentAggregat(behandlingId)
                .getGjeldendeBekreftetVersjon().flatMap(FamilieHendelseEntitet::getFødselsdato).map(VirkedagUtil::fomVirkedag)
                .filter(fødselsdatoUkedag -> fødselsdatoUkedag.isBefore(oppgittStartdato.orElse(LocalDate.MAX)));
            return evFødselFørOppgittStartdato.or(() -> oppgittStartdato);
        }
        return oppgittStartdato;
    }

    private Optional<LocalDate> getFørsteUttaksdagHvisOppgitt(Skjæringstidspunkt skjæringstidspunkter) {
        try {
            return Optional.of(skjæringstidspunkter.getFørsteUttaksdato());
        } catch (NullPointerException npe) {
            return Optional.empty();
        }
    }

    private Optional<Integer> hentDekningsgrad(BehandlingReferanse ref) {
        var fagsakRelasjonOpt = fagsakRelasjonRepository.finnRelasjonHvisEksisterer(ref.saksnummer());
        return fagsakRelasjonOpt.map(fagsakRelasjon -> fagsakRelasjon.getDekningsgrad().getVerdi());
    }
}
