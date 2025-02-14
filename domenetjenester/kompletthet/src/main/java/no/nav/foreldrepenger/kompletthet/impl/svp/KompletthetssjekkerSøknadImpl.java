package no.nav.foreldrepenger.kompletthet.impl.svp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.foreldrepenger.kompletthet.impl.KompletthetssjekkerSøknad;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
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
    public Optional<LocalDateTime> erSøknadMottattForTidlig(Skjæringstidspunkt stp) {
        var permisjonsstart = stp.getSkjæringstidspunktHvisUtledet();
        if (permisjonsstart.isPresent()) {
            var ventefrist = permisjonsstart.get().minus(ventefristForTidligSøknad);
            var erSøknadMottattForTidlig = ventefrist.isAfter(LocalDate.now());
            if (erSøknadMottattForTidlig) {
                var ventefristTidspunkt = ventefrist.atStartOfDay();
                return Optional.of(ventefristTidspunkt);
            }
        }
        return Optional.empty();
    }

    @Override
    public Boolean erSøknadMottatt(BehandlingReferanse ref) {
        var søknad = søknadRepository.hentSøknadHvisEksisterer(ref.behandlingId());
        var mottattSøknad = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(ref.fagsakId())
            .stream()
            .filter(mottattDokument -> DokumentTypeId.getSøknadTyper().contains(mottattDokument.getDokumentType()) || DokumentKategori.SØKNAD.equals(
                mottattDokument.getDokumentKategori()))
            .findFirst();
        // sjekker på både søknad og mottatte dokumenter siden søknad ikke lagres med en gang
        return søknad.isPresent() || mottattSøknad.isPresent();
    }
}
