package no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.es;

import static java.util.Collections.emptyList;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.NoResultException;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadVedleggEntitet;
import no.nav.foreldrepenger.behandlingslager.kodeverk.arkiv.DokumentType;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.vedtak.util.FPDateUtil;

@ApplicationScoped
@BehandlingTypeRef
@FagsakYtelseTypeRef("ES")
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
    public KompletthetResultat vurderSøknadMottattForTidlig(BehandlingReferanse ref) {
        throw new UnsupportedOperationException("Metode brukes ikke i ES"); //$NON-NLS-1$
    }

    @Override
    public KompletthetResultat vurderForsendelseKomplett(BehandlingReferanse ref) {
        if (utledAlleManglendeVedleggForForsendelse(ref).isEmpty()) {
            return KompletthetResultat.oppfylt();
        } else {
            AksjonspunktDefinisjon definisjon = AUTO_VENTER_PÅ_KOMPLETT_SØKNAD;
            LocalDateTime ønsketFrist = FPDateUtil.nå().plusDays(definisjon.getFristPeriod().getDays());
            return KompletthetResultat.ikkeOppfylt(ønsketFrist, Venteårsak.AVV_DOK);
        }
    }

    // Spør Joark om dokumentliste og sjekker det som finnes i vedleggslisten på søknaden mot det som ligger i Joark.
    // Vedleggslisten på søknaden regnes altså i denne omgang som fasit på hva som er påkrevd.
    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggForForsendelse(BehandlingReferanse ref) {

        final Optional<SøknadEntitet> søknad = søknadRepository.hentSøknadHvisEksisterer(ref.getBehandlingId());

        // Manuelt registrerte søknader har foreløpig ikke vedleggsliste og kan derfor ikke kompletthetssjekkes:
        if (!søknad.isPresent() || (!søknad.get().getElektroniskRegistrert() || søknad.get().getSøknadVedlegg() == null || søknad.get().getSøknadVedlegg().isEmpty())) {
            return emptyList();
        }

        Set<DokumentType> dokumentTypeIds = dokumentArkivTjeneste.hentDokumentTypeIdForSak(ref.getSaksnummer(), LocalDate.MIN, Collections.emptyList());

        return søknad.get().getSøknadVedlegg()
            .stream()
            .filter(SøknadVedleggEntitet::isErPåkrevdISøknadsdialog)
            .map(SøknadVedleggEntitet::getSkjemanummer)
            .map(this::finnDokumentTypeId)
            .filter(doc -> !dokumentTypeIds.contains(doc))
            .map(ManglendeVedlegg::new)
            .collect(Collectors.toList());
    }

    @Override
    public List<ManglendeVedlegg> utledAlleManglendeVedleggSomIkkeKommer(BehandlingReferanse ref) {
        return emptyList();
    }

    @Override
    public boolean erForsendelsesgrunnlagKomplett(BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();
        SøknadEntitet søknad = søknadRepository.hentSøknad(behandlingId);
        if (søknad == null) {
            // Uten søknad må det antas at den heller ikke er komplett. Sjekker nedenfor forutsetter at søknad finnes.
            return false;
        }
        if (!søknad.getElektroniskRegistrert()) {
            // Søknad manuelt registrert av saksbehandlier - dermed er opplysningsplikt allerede vurdert av han/henne
            return true;
        }

        List<ManglendeVedlegg> manglendeVedlegg = utledAlleManglendeVedleggForForsendelse(ref);
        if (manglendeVedlegg.isEmpty()) {
            return true;
        }
        if (familieHendelseRepository.hentAggregat(behandlingId).getSøknadVersjon().getGjelderFødsel()) {
            if (finnesBarnet(ref)) {
                return true;
            }
        }
        return false;
    }

    private boolean finnesBarnet(BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();
        final Optional<LocalDate> fødselsDato = familieHendelseRepository.hentAggregat(behandlingId).getSøknadVersjon().getBarna()
            .stream().map(UidentifisertBarn::getFødselsdato).findFirst();

        if (fødselsDato.isPresent()) {
            PersonopplysningerAggregat personopplysninger = personopplysningTjeneste.hentPersonopplysninger(ref);
            List<PersonopplysningEntitet> alleBarn = personopplysninger.getBarna();
            return alleBarn.stream().anyMatch(bb -> bb.getFødselsdato().equals(fødselsDato.get()));
        }
        return false;
    }


    private DokumentTypeId finnDokumentTypeId(String dokumentTypeIdKode) {
        DokumentTypeId dokumentTypeId;
        try {
            dokumentTypeId = DokumentTypeId.finnForKodeverkEiersKode(dokumentTypeIdKode);
        } catch (NoResultException e) { //NOSONAR
            // skal tåle dette
            dokumentTypeId = DokumentTypeId.UDEFINERT;
        }
        return dokumentTypeId;
    }
}
