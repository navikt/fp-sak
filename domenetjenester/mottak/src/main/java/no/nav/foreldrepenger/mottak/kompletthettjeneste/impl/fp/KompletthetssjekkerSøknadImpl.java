package no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.fp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.NoResultException;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadVedleggEntitet;
import no.nav.foreldrepenger.behandlingslager.kodeverk.arkiv.DokumentType;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.KompletthetssjekkerSøknad;

public abstract class KompletthetssjekkerSøknadImpl implements KompletthetssjekkerSøknad {

    private SøknadRepository søknadRepository;
    private Period ventefristForTidligSøknad;
    private MottatteDokumentRepository mottatteDokumentRepository;

    KompletthetssjekkerSøknadImpl() {
        // CDI
    }

    KompletthetssjekkerSøknadImpl(Period ventefristForTidligSøknad,
                                SøknadRepository søknadRepository,
                                MottatteDokumentRepository mottatteDokumentRepository) {
        this.ventefristForTidligSøknad = ventefristForTidligSøknad;
        this.søknadRepository = søknadRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
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

    protected List<ManglendeVedlegg> identifiserManglendeVedlegg(Optional<SøknadEntitet> søknad, Set<DokumentType> dokumentTypeIdSet) {

        return getSøknadVedleggListe(søknad)
            .stream()
            .filter(SøknadVedleggEntitet::isErPåkrevdISøknadsdialog)
            .map(SøknadVedleggEntitet::getSkjemanummer)
            .map(this::finnDokumentTypeId)
            .filter(doc -> !dokumentTypeIdSet.contains(doc))
            .map(ManglendeVedlegg::new)
            .collect(Collectors.toList());
    }

    private Set<SøknadVedleggEntitet> getSøknadVedleggListe(Optional<SøknadEntitet> søknad) {
        if (søknad.map(SøknadEntitet::getElektroniskRegistrert).orElse(false)) {
            return søknad.map(SøknadEntitet::getSøknadVedlegg)
                .orElse(Collections.emptySet());
        }
        return Collections.emptySet();
    }

    private DokumentTypeId finnDokumentTypeId(String dokumentTypeIdKode) {
        DokumentTypeId dokumentTypeId;
        try {
            dokumentTypeId = DokumentTypeId.finnForKodeverkEiersKode(dokumentTypeIdKode);
        } catch (NoResultException e) { // NOSONAR
            // skal tåle dette
            dokumentTypeId = DokumentTypeId.UDEFINERT;
        }
        return dokumentTypeId;
    }

    @Override
    public Boolean erSøknadMottatt(BehandlingReferanse ref) {
        final Optional<SøknadEntitet> søknad = søknadRepository.hentSøknadHvisEksisterer(ref.getBehandlingId());
        Optional<MottattDokument> mottattSøknad =  mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(ref.getFagsakId()).stream()
            .filter(mottattDokument -> DokumentTypeId.getSøknadTyper().contains(mottattDokument.getDokumentType().getKode())
                || DokumentKategori.SØKNAD.equals(mottattDokument.getDokumentKategori()))
            .findFirst();
        // sjekker på både søknad og mottatte dokumenter siden søknad ikke lagres med en gang
        return søknad.isPresent() || mottattSøknad.isPresent();
    }
}
