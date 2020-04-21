package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrganisasjonsNummerValidator;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Organisasjonstype;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.rest.SøknadType;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.kompletthet.KompletthetsjekkerProvider;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class SøknadDtoTjeneste {

    private BehandlingRepositoryProvider repositoryProvider;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private KompletthetsjekkerProvider kompletthetsjekkerProvider;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
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
                                 ArbeidsgiverTjeneste arbeidsgiverTjeneste,
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
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
    }

    public Optional<SoknadDto> mapFra(Behandling behandling) {
        Optional<SøknadEntitet> søknadOpt = repositoryProvider.getSøknadRepository().hentSøknadHvisEksisterer(behandling.getId());
        if (søknadOpt.isPresent()) {
            SøknadEntitet søknad = søknadOpt.get();
            var ref = BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
            var fhGrunnlag = familieHendelseRepository.hentAggregat(behandling.getId());
            if (fhGrunnlag.getSøknadVersjon().getGjelderFødsel()) {
                return lagSoknadFodselDto(søknad, fhGrunnlag.getSøknadVersjon(), ref);
            } else if (fhGrunnlag.getSøknadVersjon().getGjelderAdopsjon()) {
                return lagSoknadAdopsjonDto(søknad, fhGrunnlag.getSøknadVersjon(), ref);
            }
        }
        return Optional.empty();
    }

    private Optional<SoknadDto> lagSoknadFodselDto(SøknadEntitet søknad, FamilieHendelseEntitet familieHendelse, BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();

        SoknadFodselDto soknadFodselDto = new SoknadFodselDto();
        Map<Integer, LocalDate> fødselsdatoer = familieHendelse.getBarna().stream()
            .collect(Collectors.toMap(UidentifisertBarn::getBarnNummer, UidentifisertBarn::getFødselsdato));
        mapFellesSoknadDtoFelter(søknad, soknadFodselDto);
        soknadFodselDto.setSoknadType(SøknadType.FØDSEL);
        soknadFodselDto.setUtstedtdato(familieHendelse.getTerminbekreftelse().map(TerminbekreftelseEntitet::getUtstedtdato).orElse(null));
        soknadFodselDto.setTermindato(familieHendelse.getTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato).orElse(null));
        soknadFodselDto.setAntallBarn(familieHendelse.getAntallBarn());
        soknadFodselDto.setBegrunnelseForSenInnsending(søknad.getBegrunnelseForSenInnsending());
        soknadFodselDto.setFarSokerType(søknad.getFarSøkerType());

        personopplysningRepository.hentPersonopplysningerHvisEksisterer(behandlingId).flatMap(PersonopplysningGrunnlagEntitet::getOppgittAnnenPart).ifPresent(oap -> {
            soknadFodselDto.setAnnenPartNavn(oap.getNavn());
        });

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
        Kompletthetsjekker kompletthetsjekker = kompletthetsjekkerProvider.finnKompletthetsjekkerFor(ref.getFagsakYtelseType(), ref.getBehandlingType());
        final List<ManglendeVedlegg> alleManglendeVedlegg = kompletthetsjekker.utledAlleManglendeVedleggForForsendelse(ref);
        final List<ManglendeVedlegg> vedleggSomIkkeKommer = kompletthetsjekker.utledAlleManglendeVedleggSomIkkeKommer(ref);

        // Fjerner slik at det ikke blir dobbelt opp, og for å markere korrekt hvilke som ikke vil komme
        alleManglendeVedlegg.removeIf(e -> vedleggSomIkkeKommer.stream().anyMatch(it -> it.getArbeidsgiver().equals(e.getArbeidsgiver())));
        alleManglendeVedlegg.addAll(vedleggSomIkkeKommer);

        return alleManglendeVedlegg.stream().map(this::mapTilManglendeVedleggDto).collect(Collectors.toList());
    }

    private ManglendeVedleggDto mapTilManglendeVedleggDto(ManglendeVedlegg mv) {
        final ManglendeVedleggDto dto = new ManglendeVedleggDto();
        dto.setDokumentType(mv.getDokumentType());
        if (mv.getDokumentType().equals(DokumentTypeId.INNTEKTSMELDING)) {
            dto.setArbeidsgiver(mapTilArbeidsgiverDto(mv.getArbeidsgiver()));
            dto.setBrukerHarSagtAtIkkeKommer(mv.getBrukerHarSagtAtIkkeKommer());
        }
        return dto;
    }

    private ArbeidsgiverDto mapTilArbeidsgiverDto(String arbeidsgiverIdent) {
        if (OrganisasjonsNummerValidator.erGyldig(arbeidsgiverIdent) || Organisasjonstype.erKunstig(arbeidsgiverIdent)) {
            return virksomhetArbeidsgiver(arbeidsgiverIdent);
        } else {
            return privatpersonArbeidsgiver(arbeidsgiverIdent);
        }
    }

    private ArbeidsgiverDto privatpersonArbeidsgiver(String aktørId) {
        ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(Arbeidsgiver.person(new AktørId(aktørId)));
        ArbeidsgiverDto dto = new ArbeidsgiverDto();
        dto.setNavn(opplysninger.getNavn());
        dto.setFødselsdato(opplysninger.getFødselsdato());
        dto.setAktørId(aktørId);

        // Dette må gjøres for å ikke knekke frontend, kan fjernes når frontend er rettet.
        dto.setOrganisasjonsnummer(opplysninger.getIdentifikator());

        return dto;
    }

    private ArbeidsgiverDto virksomhetArbeidsgiver(String orgnr) {
        ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(Arbeidsgiver.virksomhet(orgnr));
        ArbeidsgiverDto dto = new ArbeidsgiverDto();
        dto.setOrganisasjonsnummer(orgnr);
        dto.setNavn(opplysninger.getNavn());
        return dto;
    }

    private Optional<SoknadDto> lagSoknadAdopsjonDto(SøknadEntitet søknad, FamilieHendelseEntitet familieHendelse, BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();
        Map<Integer, LocalDate> fødselsdatoer = familieHendelse.getBarna().stream()
            .collect(Collectors.toMap(UidentifisertBarn::getBarnNummer, UidentifisertBarn::getFødselsdato));
        SoknadAdopsjonDto soknadAdopsjonDto = new SoknadAdopsjonDto();
        mapFellesSoknadDtoFelter(søknad, soknadAdopsjonDto);
        soknadAdopsjonDto.setSoknadType(SøknadType.ADOPSJON);
        soknadAdopsjonDto.setOmsorgsovertakelseDato(familieHendelse.getAdopsjon().map(AdopsjonEntitet::getOmsorgsovertakelseDato).orElse(null));
        soknadAdopsjonDto.setBarnetsAnkomstTilNorgeDato(familieHendelse.getAdopsjon().map(AdopsjonEntitet::getAnkomstNorgeDato).orElse(null));
        soknadAdopsjonDto.setFarSokerType(søknad.getFarSøkerType());
        soknadAdopsjonDto.setAdopsjonFodelsedatoer(fødselsdatoer);
        soknadAdopsjonDto.setAntallBarn(familieHendelse.getAntallBarn());
        soknadAdopsjonDto.setBegrunnelseForSenInnsending(søknad.getBegrunnelseForSenInnsending());

        personopplysningRepository.hentPersonopplysningerHvisEksisterer(behandlingId).flatMap(PersonopplysningGrunnlagEntitet::getOppgittAnnenPart).ifPresent(oap -> {
            soknadAdopsjonDto.setAnnenPartNavn(oap.getNavn());
        });

        ytelsesfordelingRepository.hentAggregatHvisEksisterer(behandlingId).ifPresent(of -> {
            soknadAdopsjonDto.setOppgittRettighet(OppgittRettighetDto.mapFra(of.getOppgittRettighet()));
            soknadAdopsjonDto.setOppgittFordeling(OppgittFordelingDto.mapFra(of.getOppgittFordeling(), hentOppgittStartdatoForPermisjon(ref.getBehandlingId(), null)));
        });

        medlemTjeneste.hentMedlemskap(behandlingId).ifPresent(ma -> {
            soknadAdopsjonDto.setOppgittTilknytning(OppgittTilknytningDto.mapFra(ma.getOppgittTilknytning().orElse(null)));
        });

        soknadAdopsjonDto.setDekningsgrad(hentDekningsgrad(ref).orElse(null));
        soknadAdopsjonDto.setManglendeVedlegg(genererManglendeVedlegg(ref));
        return Optional.of(soknadAdopsjonDto);
    }

    /* TODO: Vurdere en util klasse for slike metoder se: VurderOpphørAvYtelser.fomMandag*/
    private static LocalDate finnNesteUkedag(LocalDate fom) {
        DayOfWeek ukedag = DayOfWeek.from(fom);
        if (DayOfWeek.SUNDAY.getValue() == ukedag.getValue())
            return fom.plusDays(1);
        if (DayOfWeek.SATURDAY.getValue() == ukedag.getValue())
            return fom.plusDays(2);
        return fom;
    }

    private Optional<LocalDate> hentOppgittStartdatoForPermisjon(Long behandlingId, RelasjonsRolleType rolleType) {
        Skjæringstidspunkt skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);

        Optional<LocalDate> oppgittStartdato = getFørsteUttaksdagHvisOppgitt(skjæringstidspunkter)
            .or(() -> skjæringstidspunkter.getSkjæringstidspunktHvisUtledet());
        if (RelasjonsRolleType.MORA.equals(rolleType)) {
            Optional<LocalDate> evFødselFørOppgittStartdato = familieHendelseRepository.hentAggregat(behandlingId)
                .getGjeldendeBekreftetVersjon().flatMap(FamilieHendelseEntitet::getFødselsdato).map(fødselsdato -> finnNesteUkedag(fødselsdato))
                .filter(fødselsdatoUkedag -> fødselsdatoUkedag.isBefore(oppgittStartdato.orElse(LocalDate.MAX)));
            return evFødselFørOppgittStartdato.or(() -> oppgittStartdato);
        } else {
            return oppgittStartdato;
        }
    }

    private Optional<LocalDate> getFørsteUttaksdagHvisOppgitt(Skjæringstidspunkt skjæringstidspunkter) {
        try {
            return Optional.of(skjæringstidspunkter.getFørsteUttaksdato());
        } catch (NullPointerException npe) {
            return Optional.empty();
        }
    }

    private Optional<Integer> hentDekningsgrad(BehandlingReferanse ref) {
        Optional<FagsakRelasjon> fagsakRelasjonOpt = fagsakRelasjonRepository.finnRelasjonHvisEksisterer(ref.getSaksnummer());
        return fagsakRelasjonOpt.map(fagsakRelasjon -> fagsakRelasjon.getDekningsgrad().getVerdi());
    }
}
