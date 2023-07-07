package no.nav.foreldrepenger.mottak.dokumentmottak;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.SøknadUtsettelseUttakDato;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.MottattDokumentPersisterer;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3.EndringUtsettelseUttak;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3.SøknadWrapper;

@ApplicationScoped
public class MottatteDokumentTjeneste {

    private Period fristForInnsendingAvDokumentasjon;

    private MottattDokumentPersisterer mottattDokumentPersisterer;
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
                                    MottattDokumentPersisterer mottattDokumentPersisterer,
                                    MottatteDokumentRepository mottatteDokumentRepository,
                                    BehandlingRepositoryProvider behandlingRepositoryProvider) {
        this.fristForInnsendingAvDokumentasjon = fristForInnsendingAvDokumentasjon;
        this.mottattDokumentPersisterer = mottattDokumentPersisterer;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.behandlingRepositoryProvider = behandlingRepositoryProvider;
    }

    public void persisterDokumentinnhold(Behandling behandling, MottattDokument dokument, Optional<LocalDate> gjelderFra) {
        oppdaterMottattDokumentMedBehandling(dokument, behandling.getId());
        if (dokument.getPayloadXml() != null) {
            @SuppressWarnings("rawtypes") var dokumentWrapper = mottattDokumentPersisterer.xmlTilWrapper(dokument);
            mottattDokumentPersisterer.persisterDokumentinnhold(dokumentWrapper, dokument, behandling, gjelderFra);
        }
    }

    public SøknadUtsettelseUttakDato finnUtsettelseUttakForSøknad(MottattDokument dokument) {
        if (!dokument.getDokumentType().erForeldrepengeSøknad() || dokument.getPayloadXml() == null) {
            return null;
        }
        var dokumentWrapper = (SøknadWrapper) mottattDokumentPersisterer.xmlTilWrapper(dokument);
        return EndringUtsettelseUttak.ekstraherUtsettelseUttakFra(dokumentWrapper);

    }

    public Long lagreMottattDokumentPåFagsak(MottattDokument dokument) {
        var mottattDokument = mottatteDokumentRepository.lagre(dokument);
        return mottattDokument.getId();
    }

    public List<MottattDokument> hentMottatteDokument(Long behandlingId) {
        return mottatteDokumentRepository.hentMottatteDokument(behandlingId);
    }

    public List<MottattDokument> hentMottatteDokumentFagsak(Long fagsakId) {
        return mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(fagsakId);
    }

    public boolean harMottattDokumentSet(Long behandlingId, Set<DokumentTypeId> dokumentTypeIdSet) {
        return hentMottatteDokument(behandlingId).stream().anyMatch(dok -> dokumentTypeIdSet.contains(dok.getDokumentType()));
    }

    public boolean harMottattDokumentKat(Long behandlingId, DokumentKategori dokumentKategori) {
        return hentMottatteDokument(behandlingId).stream().anyMatch(dok -> dokumentKategori.equals(dok.getDokumentKategori()));
    }

    public List<MottattDokument> hentMottatteDokumentVedlegg(Long behandlingId) {
        var spesialtyper = DokumentTypeId.getSpesialTyperKoder();
        return mottatteDokumentRepository.hentMottatteDokument(behandlingId).stream()
            .filter(m -> !spesialtyper.contains(m.getDokumentType()))
            .toList();
    }

    public void oppdaterMottattDokumentMedBehandling(MottattDokument mottattDokument, Long behandlingId) {
        mottatteDokumentRepository.oppdaterMedBehandling(mottattDokument, behandlingId);
    }

    public Optional<MottattDokument> hentMottattDokument(Long mottattDokumentId) {
        return mottatteDokumentRepository.hentMottattDokument(mottattDokumentId);
    }

    public boolean erSisteYtelsesbehandlingAvslåttPgaManglendeDokumentasjon(Fagsak sak) {
        Objects.requireNonNull(sak, "Fagsak");
        var behandling = behandlingRepositoryProvider.getBehandlingRepository().finnSisteAvsluttedeIkkeHenlagteBehandling(sak.getId());
        return behandling.map(this::erAvsluttetPgaManglendeDokumentasjon).orElse(Boolean.FALSE);
    }

    /**
     * Beregnes fra vedtaksdato
     */
    public boolean harFristForInnsendingAvDokGåttUt(Fagsak sak) {
        Objects.requireNonNull(sak, "Fagsak");
        var behandlingOptional = behandlingRepositoryProvider.getBehandlingRepository().finnSisteAvsluttedeIkkeHenlagteBehandling(sak.getId());
        return behandlingOptional.flatMap(b -> behandlingRepositoryProvider.getBehandlingVedtakRepository().hentForBehandlingHvisEksisterer(b.getId()))
            .map(BehandlingVedtak::getVedtaksdato)
            .map(dato -> dato.isBefore(LocalDate.now().minus(fristForInnsendingAvDokumentasjon))).orElse(Boolean.FALSE);
    }

    private boolean erAvsluttetPgaManglendeDokumentasjon(Behandling behandling) {
        Objects.requireNonNull(behandling, "Behandling");
        var bRes = behandlingRepositoryProvider.getBehandlingsresultatRepository().hentHvisEksisterer(behandling.getId());
        return bRes.filter(br -> BehandlingResultatType.AVSLÅTT.equals(br.getBehandlingResultatType()))
            .map(Behandlingsresultat::getAvslagsårsak).map(Avslagsårsak.MANGLENDE_DOKUMENTASJON::equals).orElse(Boolean.FALSE);
    }

}
