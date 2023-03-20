package no.nav.foreldrepenger.behandling.steg.mottatteopplysninger;

import static java.time.temporal.ChronoUnit.DAYS;

import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.UtlandMarkering;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;

@Dependent
public class RegistrerFagsakEgenskaper {

    private final PersoninfoAdapter personinfo;
    private final MedlemskapRepository medlemskapRepository;
    private final FagsakEgenskapRepository fagsakEgenskapRepository;

    @Inject
    public RegistrerFagsakEgenskaper(PersoninfoAdapter personinfo, MedlemskapRepository medlemskapRepository, FagsakEgenskapRepository fagsakEgenskapRepository) {
        this.personinfo = personinfo;
        this.medlemskapRepository = medlemskapRepository;
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
    }

    public UtlandMarkering registrerFagsakEgenskaper(Behandling behandling, boolean oppgittRelasjonTilEØS) {
        if (!BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType()) ||
            fagsakEgenskapRepository.finnUtlandMarkering(behandling.getFagsakId()).isPresent()) {
            return fagsakEgenskapRepository.finnUtlandMarkering(behandling.getFagsakId()).orElse(UtlandMarkering.NASJONAL);
        }
        var geografiskTilknyttetUtlandEllerUkjent = personinfo.harGeografiskTilknytningUtland(behandling.getAktørId());
        var medlemskapFramtidigLangtOppholdUtlands = medlemskapRepository.hentMedlemskap(behandling.getId())
            .flatMap(MedlemskapAggregat::getOppgittTilknytning)
            .map(MedlemskapOppgittTilknytningEntitet::getOpphold).orElse(Set.of()).stream()
            .filter(land -> !land.isTidligereOpphold())
            .anyMatch(land -> Math.abs(DAYS.between(land.getPeriodeFom(), land.getPeriodeTom())) > 350);

        var utlandMarkering = UtlandMarkering.NASJONAL;
        if (oppgittRelasjonTilEØS) {
            utlandMarkering = UtlandMarkering.EØS_BOSATT_NORGE;
        } else if (geografiskTilknyttetUtlandEllerUkjent || medlemskapFramtidigLangtOppholdUtlands) {
            utlandMarkering = UtlandMarkering.BOSATT_UTLAND;
        }
        if (!UtlandMarkering.NASJONAL.equals(utlandMarkering)) {
            fagsakEgenskapRepository.lagreEgenskapUtenHistorikk(behandling.getFagsakId(), utlandMarkering);
        }
        return utlandMarkering;
    }

    public boolean harVurdertInnhentingDokumentasjon(Behandling behandling) {
        return fagsakEgenskapRepository.finnUtlandDokumentasjonStatus(behandling.getFagsakId()).isPresent();
    }


}
