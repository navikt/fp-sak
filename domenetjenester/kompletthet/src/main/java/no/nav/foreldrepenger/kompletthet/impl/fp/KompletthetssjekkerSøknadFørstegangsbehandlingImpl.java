package no.nav.foreldrepenger.kompletthet.impl.fp;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

@ApplicationScoped
@BehandlingTypeRef(BehandlingType.FØRSTEGANGSSØKNAD)
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
public class KompletthetssjekkerSøknadFørstegangsbehandlingImpl extends KompletthetssjekkerSøknadImpl {
    private static final Logger LOG = LoggerFactory.getLogger(KompletthetssjekkerSøknadFørstegangsbehandlingImpl.class);

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
     * @param ref behandlingen
     * @return Liste over manglende vedlegg
     */
    @Override
    public List<ManglendeVedlegg> utledManglendeVedleggForSøknad(BehandlingReferanse ref) {
        var søknad = søknadRepository.hentSøknadHvisEksisterer(ref.behandlingId());
        var dokumentTypeIds = dokumentArkivTjeneste.hentDokumentTypeIdForSak(ref.saksnummer(), LocalDate.MIN);
        var manglendeVedlegg = identifiserManglendeVedlegg(søknad, dokumentTypeIds);

        if (!manglendeVedlegg.isEmpty()) {
            LOG.info("Behandling {} er ikke komplett - mangler følgende vedlegg til søknad: {}", ref.behandlingId(),
                lagDokumentTypeString(manglendeVedlegg));
        }

        return manglendeVedlegg;
    }

    private String lagDokumentTypeString(List<ManglendeVedlegg> manglendeVedlegg) {
        return manglendeVedlegg.stream().map(mv -> mv.getDokumentType().getKode()).toList().toString();
    }
}
