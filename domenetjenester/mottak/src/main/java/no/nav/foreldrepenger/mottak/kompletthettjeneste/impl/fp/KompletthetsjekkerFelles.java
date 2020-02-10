package no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.fp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerApplikasjonTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.vedtak.util.FPDateUtil;

/**
 * Fellesklasse for gjenbrukte metode av subklasser for {@link KompletthetsjekkerImpl}.
 * <p>
 * Favor composition over inheritance
 */
@ApplicationScoped
public class KompletthetsjekkerFelles {


    /**
     * Disse konstantene ligger hardkodet (og ikke i KonfigVerdi), da endring i en eller flere av disse vil
     * sannsynnlig kreve kodeendring
     */
    public static final Integer VENTEFRIST_FRAM_I_TID_FRA_MOTATT_DATO_UKER = 3;
    public static final Integer VENTEFRIST_FOR_MANGLENDE_SØKNAD = 4;

    private SøknadRepository søknadRepository;
    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private BehandlingRepository behandlingRepository;

    KompletthetsjekkerFelles() {
        // CDI
    }

    @Inject
    public KompletthetsjekkerFelles(BehandlingRepositoryProvider provider,
                                      DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste,
                                      DokumentBehandlingTjeneste dokumentBehandlingTjeneste) {
        this.søknadRepository = provider.getSøknadRepository();
        this.behandlingRepository = provider.getBehandlingRepository();
        this.dokumentBestillerApplikasjonTjeneste = dokumentBestillerApplikasjonTjeneste;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
    }

    public Behandling hentBehandling(Long behandlingId) {
        return behandlingRepository.hentBehandling(behandlingId);
    }

    public Optional<LocalDateTime> finnVentefristTilForTidligMottattSøknad(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId må være satt"); // NOSONAR //$NON-NLS-1$
        SøknadEntitet søknad = søknadRepository.hentSøknad(behandlingId);
        Objects.requireNonNull(søknad, "søknad kan ikke være null"); // NOSONAR //$NON-NLS-1$

        final LocalDate ønsketFrist = søknad.getMottattDato().plusWeeks(VENTEFRIST_FRAM_I_TID_FRA_MOTATT_DATO_UKER);
        return finnVentefrist(ønsketFrist);
    }

    public Optional<LocalDateTime> finnVentefrist(LocalDate ønsketFrist) {
        if (ønsketFrist.isAfter(FPDateUtil.iDag())) {
            LocalDateTime ventefrist = ønsketFrist.atStartOfDay();
            return Optional.of(ventefrist);
        }
        return Optional.empty();
    }

    public LocalDateTime finnVentefristTilManglendeSøknad() {
        return FPDateUtil.nå().plusWeeks(VENTEFRIST_FOR_MANGLENDE_SØKNAD);
    }

    public void sendBrev(Long behandlingId, DokumentMalType dokumentMalType, String årsakskode) {
        if (!dokumentBehandlingTjeneste.erDokumentProdusert(behandlingId, dokumentMalType)) {
            BestillBrevDto bestillBrevDto = new BestillBrevDto(behandlingId, dokumentMalType, null, årsakskode);
            dokumentBestillerApplikasjonTjeneste.bestillDokument(bestillBrevDto, HistorikkAktør.VEDTAKSLØSNINGEN, false);
        }
    }

    public boolean erSendtBrev(Long behandlingId, DokumentMalType dokumentMalType) {
        return dokumentBehandlingTjeneste.erDokumentProdusert(behandlingId, dokumentMalType);
    }
}
