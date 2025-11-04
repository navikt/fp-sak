package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SøknadsperiodeFristTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@ApplicationScoped
public class SøknadDtoTjenesteOld {

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private SøknadsperiodeFristTjeneste fristTjeneste;
    private Kompletthetsjekker kompletthetsjekker;
    private FamilieHendelseRepository familieHendelseRepository;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;
    private SøknadRepository søknadRepository;
    private MedlemTjeneste medlemTjeneste;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private BehandlingRepository behandlingRepository;

    protected SøknadDtoTjenesteOld() {
        // for CDI proxy
    }

    @Inject
    public SøknadDtoTjenesteOld(BehandlingRepositoryProvider repositoryProvider,
                                SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                SøknadsperiodeFristTjeneste fristTjeneste,
                                Kompletthetsjekker kompletthetsjekker,
                                YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                MedlemTjeneste medlemTjeneste,
                                FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.fristTjeneste = fristTjeneste;
        this.kompletthetsjekker = kompletthetsjekker;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.medlemTjeneste = medlemTjeneste;
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.uttaksperiodegrenseRepository = repositoryProvider.getUttaksperiodegrenseRepository();
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    public Optional<SoknadDto> mapFra(Behandling behandling) {
        return søknadRepository.hentSøknadHvisEksisterer(behandling.getId())
            .map(s -> getSøknadDto(behandling, s));
    }

    private SoknadDto getSøknadDto(Behandling behandling, SøknadEntitet søknad) {
        var ref = BehandlingReferanse.fra(behandling);
        var fhGrunnlag = familieHendelseRepository.hentAggregat(behandling.getId());
        if (fhGrunnlag.getSøknadVersjon().getGjelderFødsel()) {
            return lagSoknadFodselDto(søknad, fhGrunnlag.getSøknadVersjon(), ref);
        } else {
            return lagSoknadAdopsjonDto(søknad, fhGrunnlag.getSøknadVersjon(), ref);
        }
    }

    private SoknadDto lagSoknadFodselDto(SøknadEntitet søknad, FamilieHendelseEntitet familieHendelse, BehandlingReferanse ref) {
        var behandlingId = ref.behandlingId();

        var soknadFodselDto = new SoknadFodselDto();
        var fødselsdatoer = familieHendelse.getBarna().stream()
            .collect(Collectors.toMap(UidentifisertBarn::getBarnNummer, UidentifisertBarn::getFødselsdato));
        mapFellesSoknadDtoFelter(ref, søknad, soknadFodselDto);
        soknadFodselDto.setSoknadType(SøknadType.FØDSEL);
        soknadFodselDto.setUtstedtdato(familieHendelse.getTerminbekreftelse().map(TerminbekreftelseEntitet::getUtstedtdato).orElse(null));
        soknadFodselDto.setTermindato(familieHendelse.getTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato).orElse(null));
        soknadFodselDto.setAntallBarn(familieHendelse.getAntallBarn());
        soknadFodselDto.setBegrunnelseForSenInnsending(søknad.getBegrunnelseForSenInnsending());
        soknadFodselDto.setFarSokerType(søknad.getFarSøkerType());

        medlemTjeneste.hentMedlemskap(behandlingId)
            .ifPresent(ma -> soknadFodselDto.setOppgittTilknytning(OppgittTilknytningDto.mapFra(ma.getOppgittTilknytning().orElse(null))));

        soknadFodselDto.setManglendeVedlegg(genererManglendeVedlegg(ref));
        soknadFodselDto.setFodselsdatoer(fødselsdatoer);

        return soknadFodselDto;
    }

    private Optional<OppgittFordelingDto.OppgittDekningsgradDto> finnAnnenPartsOppgittDekningsgrad(BehandlingReferanse ref) {
        return fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(ref.fagsakId())
            .flatMap(fr -> fr.getRelatertFagsakFraId(ref.fagsakId()))
            .flatMap(annenPartsFagsak -> behandlingRepository.hentSisteYtelsesBehandlingForFagsakIdReadOnly(annenPartsFagsak.getId())
                .flatMap(b -> ytelseFordelingTjeneste.hentAggregatHvisEksisterer(b.getId()).map(yfa -> {
                    var søknadEntitet = søknadRepository.hentSøknadHvisEksisterer(b.getId());
                    var søknadDato = søknadEntitet.map(SøknadEntitet::getSøknadsdato).orElse(null);
                    var søktDekningsgrad = yfa.getOppgittDekningsgrad().getVerdi();
                    return new OppgittFordelingDto.OppgittDekningsgradDto(søknadDato, søktDekningsgrad);
                })));
    }

    private void mapFellesSoknadDtoFelter(BehandlingReferanse ref, SøknadEntitet søknad, SoknadDto soknadDto) {
        soknadDto.setMottattDato(søknad.getMottattDato());
        var gjeldendeMottattDato = uttaksperiodegrenseRepository.hentHvisEksisterer(ref.behandlingId())
            .map(Uttaksperiodegrense::getMottattDato)
            .orElseGet(søknad::getMottattDato);
        var frister = fristTjeneste.finnSøknadsfrist(ref.behandlingId());
        var fristDto = new SøknadsfristDto();
        fristDto.setMottattDato(gjeldendeMottattDato);
        fristDto.setUtledetSøknadsfrist(frister.getUtledetSøknadsfrist());
        Optional.ofNullable(frister.getSøknadGjelderPeriode()).map(LocalDateInterval::getFomDato).ifPresent(fristDto::setSøknadsperiodeStart);
        Optional.ofNullable(frister.getSøknadGjelderPeriode()).map(LocalDateInterval::getTomDato).ifPresent(fristDto::setSøknadsperiodeSlutt);
        Optional.ofNullable(frister.getDagerOversittetFrist()).ifPresent(fristDto::setDagerOversittetFrist);
        soknadDto.setSøknadsfrist(fristDto);

        ytelseFordelingTjeneste.hentAggregatHvisEksisterer(ref.behandlingId()).ifPresent(yfa -> {
            var startdato = hentOppgittStartdatoForPermisjon(ref.behandlingId(), søknad.getRelasjonsRolleType());
            var søkerOppgitt = new OppgittFordelingDto.OppgittDekningsgradDto(søknad.getSøknadsdato(), yfa.getOppgittDekningsgrad().getVerdi());
            var annenPartOppgitt = finnAnnenPartsOppgittDekningsgrad(ref);
            var avklartDekningsgrad = Optional.ofNullable(yfa.getSakskompleksDekningsgrad()).map(Dekningsgrad::getVerdi).orElse(null);
            var dekningsgrader = new OppgittFordelingDto.DekningsgradInfoDto(avklartDekningsgrad, søkerOppgitt, annenPartOppgitt.orElse(null));
            soknadDto.setOppgittFordeling(new OppgittFordelingDto(startdato.orElse(null), dekningsgrader));
        });
    }

    private List<ManglendeVedleggDto> genererManglendeVedlegg(BehandlingReferanse ref) {
        var alleManglendeVedlegg = new ArrayList<>(kompletthetsjekker.utledAlleManglendeVedleggForForsendelse(ref));
        var vedleggSomIkkeKommer = kompletthetsjekker.utledAlleManglendeInntektsmeldingerSomIkkeKommer(ref);

        // Fjerner slik at det ikke blir dobbelt opp, og for å markere korrekt hvilke som ikke vil komme
        alleManglendeVedlegg.removeIf(e -> vedleggSomIkkeKommer.stream().anyMatch(it -> it.arbeidsgiver().equals(e.arbeidsgiver())));
        alleManglendeVedlegg.addAll(vedleggSomIkkeKommer);

        return alleManglendeVedlegg.stream().map(this::mapTilManglendeVedleggDto).toList();
    }

    private ManglendeVedleggDto mapTilManglendeVedleggDto(ManglendeVedlegg mv) {
        if (mv.dokumentType().equals(DokumentTypeId.INNTEKTSMELDING)) {
            return new ManglendeVedleggDto(mv.dokumentType(), mv.arbeidsgiver(), mv.brukerHarSagtAtIkkeKommer());
        } else {
            return new ManglendeVedleggDto(mv.dokumentType());
        }
    }

    private SoknadDto lagSoknadAdopsjonDto(SøknadEntitet søknad, FamilieHendelseEntitet familieHendelse, BehandlingReferanse ref) {
        var behandlingId = ref.behandlingId();
        var fødselsdatoer = familieHendelse.getBarna().stream()
            .collect(Collectors.toMap(UidentifisertBarn::getBarnNummer, UidentifisertBarn::getFødselsdato));
        var soknadAdopsjonDto = new SoknadAdopsjonDto();
        mapFellesSoknadDtoFelter(ref, søknad, soknadAdopsjonDto);
        soknadAdopsjonDto.setSoknadType(SøknadType.ADOPSJON);
        soknadAdopsjonDto.setOmsorgsovertakelseDato(familieHendelse.getAdopsjon().map(AdopsjonEntitet::getOmsorgsovertakelseDato).orElse(null));
        soknadAdopsjonDto.setBarnetsAnkomstTilNorgeDato(familieHendelse.getAdopsjon().map(AdopsjonEntitet::getAnkomstNorgeDato).orElse(null));
        soknadAdopsjonDto.setFarSokerType(søknad.getFarSøkerType());
        soknadAdopsjonDto.setAdopsjonFodelsedatoer(fødselsdatoer);
        soknadAdopsjonDto.setAntallBarn(familieHendelse.getAntallBarn());
        soknadAdopsjonDto.setBegrunnelseForSenInnsending(søknad.getBegrunnelseForSenInnsending());

        medlemTjeneste.hentMedlemskap(behandlingId).ifPresent(ma -> soknadAdopsjonDto.setOppgittTilknytning(OppgittTilknytningDto.mapFra(ma.getOppgittTilknytning().orElse(null))));

        soknadAdopsjonDto.setManglendeVedlegg(genererManglendeVedlegg(ref));
        return soknadAdopsjonDto;
    }

    private Optional<LocalDate> hentOppgittStartdatoForPermisjon(Long behandlingId, RelasjonsRolleType rolleType) {
        try {
            var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);

            var oppgittStartdato = skjæringstidspunkter.getFørsteUttaksdatoHvisFinnes()
                .or(skjæringstidspunkter::getSkjæringstidspunktHvisUtledet);
            if (RelasjonsRolleType.MORA.equals(rolleType) && skjæringstidspunkter.gjelderFødsel()) {
                var evFødselFørOppgittStartdato = familieHendelseRepository.hentAggregat(behandlingId)
                    .getGjeldendeBekreftetVersjon().flatMap(FamilieHendelseEntitet::getFødselsdato).map(VirkedagUtil::fomVirkedag)
                    .filter(fødselsdatoUkedag -> fødselsdatoUkedag.isBefore(oppgittStartdato.orElse(LocalDate.MAX)));
                return evFødselFørOppgittStartdato.or(() -> oppgittStartdato);
            }
            return oppgittStartdato;
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}
