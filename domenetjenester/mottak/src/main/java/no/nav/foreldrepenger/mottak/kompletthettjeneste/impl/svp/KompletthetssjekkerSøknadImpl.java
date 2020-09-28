package no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.svp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.KompletthetssjekkerSøknad;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
@FagsakYtelseTypeRef("SVP")
public class KompletthetssjekkerSøknadImpl implements KompletthetssjekkerSøknad {

    private SøknadRepository søknadRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private Period ventefristForTidligSøknad;

    public KompletthetssjekkerSøknadImpl() {
        //CDI
    }

    @Inject
    public KompletthetssjekkerSøknadImpl(SøknadRepository søknadRepository,
                                         MottatteDokumentRepository mottatteDokumentRepository,
                                         @KonfigVerdi(value = "svp.ventefrist.tidlg.soeknad", defaultVerdi = "P4W") Period ventefristForTidligSøknad) {
        this.søknadRepository = søknadRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.ventefristForTidligSøknad = ventefristForTidligSøknad;
    }

    @Override
    public List<ManglendeVedlegg> utledManglendeVedleggForSøknad(BehandlingReferanse ref) {
        return Collections.emptyList(); //Påkrevde vedlegg håndheves i søknadsdialogen
    }

    @Override
    public Optional<LocalDateTime> erSøknadMottattForTidlig(BehandlingReferanse ref) {
        Optional<LocalDate> permisjonsstart = ref.getSkjæringstidspunkt().getSkjæringstidspunktHvisUtledet();
        if (permisjonsstart.isPresent()) {
            LocalDate ventefrist = permisjonsstart.get().minus(ventefristForTidligSøknad);
            boolean erSøknadMottattForTidlig = ventefrist.isAfter(LocalDate.now());
            if (erSøknadMottattForTidlig) {
                LocalDateTime ventefristTidspunkt = ventefrist.atStartOfDay();
                return Optional.of(ventefristTidspunkt);
            }
        }
        return Optional.empty();
    }

    @Override
    public Boolean erSøknadMottatt(BehandlingReferanse ref) {
        final Optional<SøknadEntitet> søknad = søknadRepository.hentSøknadHvisEksisterer(ref.getBehandlingId());
        Optional<MottattDokument> mottattSøknad = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(ref.getFagsakId()).stream()
            .filter(mottattDokument -> DokumentTypeId.getSøknadTyper().contains(mottattDokument.getDokumentType())
                || DokumentKategori.SØKNAD.equals(mottattDokument.getDokumentKategori()))
            .findFirst();
        // sjekker på både søknad og mottatte dokumenter siden søknad ikke lagres med en gang
        return søknad.isPresent() || mottattSøknad.isPresent();
    }
}
