package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.foreldrepenger.skjæringstidspunkt.SøknadsperiodeFristTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@ApplicationScoped
public class SøknadDtoTjeneste {

    private SøknadsperiodeFristTjeneste fristTjeneste;
    private Kompletthetsjekker kompletthetsjekker;
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;
    private SøknadRepository søknadRepository;

    @Inject
    public SøknadDtoTjeneste(BehandlingRepositoryProvider repositoryProvider,
                             SøknadsperiodeFristTjeneste fristTjeneste,
                             Kompletthetsjekker kompletthetsjekker) {
        this.fristTjeneste = fristTjeneste;
        this.kompletthetsjekker = kompletthetsjekker;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.uttaksperiodegrenseRepository = repositoryProvider.getUttaksperiodegrenseRepository();
    }

    SøknadDtoTjeneste() {
        // for CDI proxy
    }

    public Optional<SøknadDto> lagDto(Behandling behandling) {
        return søknadRepository.hentSøknadHvisEksisterer(behandling.getId()).map(s -> lagSøknadDto(behandling, s));
    }

    private SøknadDto lagSøknadDto(Behandling behandling, SøknadEntitet søknad) {
        var ref = BehandlingReferanse.fra(behandling);
        var behandlingId = ref.behandlingId();

        var frist = fristTjeneste.finnSøknadsfrist(behandlingId);
        var gjeldendeMottattDato = uttaksperiodegrenseRepository.hentHvisEksisterer(ref.behandlingId())
            .map(Uttaksperiodegrense::getMottattDato)
            .orElseGet(søknad::getMottattDato);

        var utledetSøknadsfrist = frist.getUtledetSøknadsfrist();
        var søknadsperiodeStart = Optional.ofNullable(frist.getSøknadGjelderPeriode()).map(LocalDateInterval::getFomDato).orElse(null);
        var søknadsperiodeSlutt = Optional.ofNullable(frist.getSøknadGjelderPeriode()).map(LocalDateInterval::getTomDato).orElse(null);
        var dagerOversittetFrist = Optional.ofNullable(frist.getDagerOversittetFrist()).orElse(0L);
        var søknadsfrist = new SøknadDto.Søknadsfrist(gjeldendeMottattDato, utledetSøknadsfrist, søknadsperiodeStart, søknadsperiodeSlutt,
            dagerOversittetFrist);

        return new SøknadDto(søknad.getMottattDato(), søknad.getBegrunnelseForSenInnsending(), lagManglendeVedleggDto(ref), søknadsfrist);
    }

    private List<ManglendeVedleggDto> lagManglendeVedleggDto(BehandlingReferanse ref) {
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
}
