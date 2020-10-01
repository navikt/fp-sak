package no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.fp;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
@BehandlingTypeRef("BT-002")
@FagsakYtelseTypeRef("FP")
public class KompletthetssjekkerSøknadFørstegangsbehandlingImpl extends KompletthetssjekkerSøknadImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(KompletthetssjekkerSøknadFørstegangsbehandlingImpl.class);

    private SøknadRepository søknadRepository;
    private DokumentArkivTjeneste dokumentArkivTjeneste;

    KompletthetssjekkerSøknadFørstegangsbehandlingImpl() {
    }

    @Inject
    public KompletthetssjekkerSøknadFørstegangsbehandlingImpl(DokumentArkivTjeneste dokumentArkivTjeneste,
                                                            BehandlingRepositoryProvider repositoryProvider,
                                                            @KonfigVerdi(value = "fp.ventefrist.tidlig.soeknad", defaultVerdi = "P4W") Period ventefristForTidligSøknad) {
        super(ventefristForTidligSøknad,
            repositoryProvider.getSøknadRepository(), repositoryProvider.getMottatteDokumentRepository());
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
    }

    /**
     * Spør Joark om dokumentliste og sjekker det som finnes i vedleggslisten på søknaden mot det som ligger i Joark.
     * Vedleggslisten på søknaden regnes altså i denne omgang som fasit på hva som er påkrevd.
     *
     * @param behandling
     * @return Liste over manglende vedlegg
     */
    @Override
    public List<ManglendeVedlegg> utledManglendeVedleggForSøknad(BehandlingReferanse ref) {
        final Optional<SøknadEntitet> søknad = søknadRepository.hentSøknadHvisEksisterer(ref.getBehandlingId());
        Set<DokumentTypeId> dokumentTypeIds = dokumentArkivTjeneste.hentDokumentTypeIdForSak(ref.getSaksnummer(), LocalDate.MIN);
        List<ManglendeVedlegg> manglendeVedlegg = identifiserManglendeVedlegg(søknad, dokumentTypeIds);

        if (!manglendeVedlegg.isEmpty()) {
            LOGGER.info("Behandling {} er ikke komplett - mangler følgende vedlegg til søknad: {}", ref.getBehandlingId(),
                lagDokumentTypeString(manglendeVedlegg)); // NOSONAR //$NON-NLS-1$
        }

        return manglendeVedlegg;
    }

    private String lagDokumentTypeString(List<ManglendeVedlegg> manglendeVedlegg) {
        return manglendeVedlegg.stream().map(mv -> mv.getDokumentType().getKode()).collect(Collectors.toList()).toString();
    }
}
