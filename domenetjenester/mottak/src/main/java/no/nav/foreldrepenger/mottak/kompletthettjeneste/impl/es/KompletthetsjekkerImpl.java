package no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.es;

import static java.util.Collections.emptyList;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadVedleggEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;

@ApplicationScoped
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
public class KompletthetsjekkerImpl implements Kompletthetsjekker {
    private SøknadRepository søknadRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private DokumentArkivTjeneste dokumentArkivTjeneste;

    KompletthetsjekkerImpl() {
        // CDI
    }

    @Inject
    public KompletthetsjekkerImpl(BehandlingRepositoryProvider repositoryProvider,
                                DokumentArkivTjeneste dokumentArkivTjeneste,
                                PersonopplysningTjeneste personopplysningTjeneste) {
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    @Override
    public KompletthetResultat vurderSøknadMottatt(BehandlingReferanse ref) {
        // Denne vil alltid være oppfylt for engangsstønad
        return KompletthetResultat.oppfylt();
    }

    @Override
    public KompletthetResultat vurderSøknadMottattForTidlig(Skjæringstidspunkt stp) {
        throw new UnsupportedOperationException("Metode brukes ikke i ES");
    }

    @Override
    public KompletthetResultat vurderForsendelseKomplett(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        if (utledAlleManglendeVedleggForForsendelse(ref).isEmpty()) {
            return KompletthetResultat.oppfylt();
        }
        var ønsketFrist = LocalDateTime.now().plus(AUTO_VENTER_PÅ_KOMPLETT_SØKNAD.getFristPeriod());
        return KompletthetResultat.ikkeOppfylt(ønsketFrist, Venteårsak.AVV_DOK);
    }

    // Spør Joark om dokumentliste og sjekker det som finnes i vedleggslisten på søknaden mot det som ligger i Joark.
    // Vedleggslisten på søknaden regnes altså i denne omgang som fasit på hva som er påkrevd.
    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggForForsendelse(BehandlingReferanse ref) {

        var søknad = søknadRepository.hentSøknadHvisEksisterer(ref.behandlingId());

        // Manuelt registrerte søknader har foreløpig ikke vedleggsliste og kan derfor ikke kompletthetssjekkes:
        if (søknad.isEmpty() || !søknad.get().getElektroniskRegistrert() || søknad.get().getSøknadVedlegg() == null || søknad.get()
            .getSøknadVedlegg()
            .isEmpty()) {
            return emptyList();
        }

        var dokumentTypeIds = dokumentArkivTjeneste.hentDokumentTypeIdForSak(ref.saksnummer(), LocalDate.MIN);

        return søknad.get().getSøknadVedlegg()
            .stream()
            .filter(SøknadVedleggEntitet::isErPåkrevdISøknadsdialog)
            .map(SøknadVedleggEntitet::getSkjemanummer)
            .map(this::finnDokumentTypeId)
            .filter(doc -> !dokumentTypeIds.contains(doc))
            .map(ManglendeVedlegg::new)
            .toList();
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggSomIkkeKommer(BehandlingReferanse ref) {
        return emptyList();
    }

    @Override
    public boolean erForsendelsesgrunnlagKomplett(BehandlingReferanse ref) {
        var behandlingId = ref.behandlingId();
        var søknad = søknadRepository.hentSøknad(behandlingId);
        if (søknad == null) {
            // Uten søknad må det antas at den heller ikke er komplett. Sjekker nedenfor forutsetter at søknad finnes.
            return false;
        }
        if (!søknad.getElektroniskRegistrert()) {
            // Søknad manuelt registrert av saksbehandlier - dermed er opplysningsplikt allerede vurdert av han/henne
            return true;
        }

        var manglendeVedlegg = utledAlleManglendeVedleggForForsendelse(ref);
        if (manglendeVedlegg.isEmpty()) {
            return true;
        }
        if (familieHendelseRepository.hentAggregat(behandlingId).getSøknadVersjon().getGjelderFødsel()) {
            return finnesBarnet(ref);
        }
        return false;
    }

    private boolean finnesBarnet(BehandlingReferanse ref) {
        var behandlingId = ref.behandlingId();
        var fødselsDato = familieHendelseRepository.hentAggregat(behandlingId)
            .getSøknadVersjon()
            .getBarna()
            .stream()
            .map(UidentifisertBarn::getFødselsdato)
            .findFirst();

        if (fødselsDato.isPresent()) {
            var personopplysninger = personopplysningTjeneste.hentPersonopplysninger(ref);
            var alleBarn = personopplysninger.getBarna();
            return alleBarn.stream().anyMatch(bb -> bb.getFødselsdato().equals(fødselsDato.get()));
        }
        return false;
    }


    private DokumentTypeId finnDokumentTypeId(String dokumentTypeIdKode) {
        DokumentTypeId dokumentTypeId;
        try {
            dokumentTypeId = DokumentTypeId.finnForKodeverkEiersKode(dokumentTypeIdKode);
        } catch (NoResultException e) {
            // skal tåle dette
            dokumentTypeId = DokumentTypeId.UDEFINERT;
        }
        return dokumentTypeId;
    }
}
