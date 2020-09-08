package no.nav.foreldrepenger.mottak.dokumentmottak;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.DokumentPersistererTjeneste;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.MottattDokumentWrapper;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class MottatteDokumentTjeneste {

    private Period fristForInnsendingAvDokumentasjon;

    private DokumentPersistererTjeneste dokumentPersistererTjeneste;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private BehandlingRepositoryProvider behandlingRepositoryProvider;

    MottatteDokumentTjeneste() {
        // for CDI proxy
    }

    /**
     *
     * @param fristForInnsendingAvDokumentasjon - Frist i uker fom siste vedtaksdato
     */
    @Inject
    public MottatteDokumentTjeneste(@KonfigVerdi(value = "sak.frist.innsending.dok", defaultVerdi = "P6W") Period fristForInnsendingAvDokumentasjon,
                                    DokumentPersistererTjeneste dokumentPersistererTjeneste,
                                    MottatteDokumentRepository mottatteDokumentRepository,
                                    BehandlingRepositoryProvider behandlingRepositoryProvider) {
        this.fristForInnsendingAvDokumentasjon = fristForInnsendingAvDokumentasjon;
        this.dokumentPersistererTjeneste = dokumentPersistererTjeneste;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.behandlingRepositoryProvider = behandlingRepositoryProvider;
    }

    public void persisterDokumentinnhold(Behandling behandling, MottattDokument dokument, Optional<LocalDate> gjelderFra) {
        oppdaterMottattDokumentMedBehandling(dokument, behandling.getId());
        if (dokument.getPayloadXml() != null) {
            @SuppressWarnings("rawtypes")
            MottattDokumentWrapper dokumentWrapper = dokumentPersistererTjeneste.xmlTilWrapper(dokument);
            dokumentPersistererTjeneste.persisterDokumentinnhold(dokumentWrapper, dokument, behandling, gjelderFra);
        }
    }

    public Long lagreMottattDokumentPåFagsak(MottattDokument dokument) {
        MottattDokument mottattDokument = mottatteDokumentRepository.lagre(dokument);
        return mottattDokument.getId();
    }

    public List<MottattDokument> hentMottatteDokument(Long behandlingId) {
        return mottatteDokumentRepository.hentMottatteDokument(behandlingId);
    }

    public List<MottattDokument> hentMottatteDokumentFagsak(Long fagsakId) {
        return mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(fagsakId);
    }

    public boolean harMottattDokumentSet(Long behandlingId, Set<String> dokumentTypeIdSet) {
        return hentMottatteDokument(behandlingId).stream().anyMatch(dok -> dokumentTypeIdSet.contains(dok.getDokumentType().getKode()));
    }

    public boolean harMottattDokumentKat(Long behandlingId, DokumentKategori dokumentKategori) {
        return hentMottatteDokument(behandlingId).stream().anyMatch(dok -> dokumentKategori.equals(dok.getDokumentKategori()));
    }

    public List<MottattDokument> hentMottatteDokumentVedlegg(Long behandlingId) {
        return mottatteDokumentRepository.hentMottatteDokumentAndreTyperPåBehandlingId(behandlingId);
    }

    public void oppdaterMottattDokumentMedBehandling(MottattDokument mottattDokument, Long behandlingId) {
        mottatteDokumentRepository.oppdaterMedBehandling(mottattDokument, behandlingId);
    }

    public Optional<MottattDokument> hentMottattDokument(Long mottattDokumentId) {
        return mottatteDokumentRepository.hentMottattDokument(mottattDokumentId);
    }

    public boolean erSisteYtelsesbehandlingAvslåttPgaManglendeDokumentasjon(Fagsak sak) {
        Objects.requireNonNull(sak, "Fagsak");
        Optional<Behandling> behandling = behandlingRepositoryProvider.getBehandlingRepository().finnSisteAvsluttedeIkkeHenlagteBehandling(sak.getId());
        return behandling.map(this::erAvsluttetPgaManglendeDokumentasjon).orElse(Boolean.FALSE);
    }

    /**
     * Beregnes fra vedtaksdato
     */
    public boolean harFristForInnsendingAvDokGåttUt(Fagsak sak) {
        Objects.requireNonNull(sak, "Fagsak");
        Optional<Behandling> behandlingOptional = behandlingRepositoryProvider.getBehandlingRepository().finnSisteAvsluttedeIkkeHenlagteBehandling(sak.getId());
        return behandlingOptional.flatMap(b -> behandlingRepositoryProvider.getBehandlingVedtakRepository().hentBehandlingvedtakForBehandlingId(b.getId()))
            .map(BehandlingVedtak::getVedtaksdato)
            .map(dato -> dato.isBefore(LocalDate.now().minus(fristForInnsendingAvDokumentasjon))).orElse(Boolean.FALSE);
    }

    private boolean erAvsluttetPgaManglendeDokumentasjon(Behandling behandling) {
        Objects.requireNonNull(behandling, "Behandling");
        Optional<Behandlingsresultat> bRes = behandlingRepositoryProvider.getBehandlingsresultatRepository().hentHvisEksisterer(behandling.getId());
        return bRes.filter(br -> BehandlingResultatType.AVSLÅTT.equals(br.getBehandlingResultatType()))
            .map(Behandlingsresultat::getAvslagsårsak).map(Avslagsårsak.MANGLENDE_DOKUMENTASJON::equals).orElse(Boolean.FALSE);
    }

}
