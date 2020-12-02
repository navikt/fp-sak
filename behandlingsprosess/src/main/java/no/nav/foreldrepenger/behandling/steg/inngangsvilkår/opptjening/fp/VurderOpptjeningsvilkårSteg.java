package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.fp;

import static java.time.LocalDateTime.of;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.MapTilOpptjeningAktiviteter;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.felles.VurderOpptjeningsvilkårStegFelles;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetKlassifisering;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultat;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsvilkårResultat;

@BehandlingStegRef(kode = "VURDER_OPPTJ")
@BehandlingTypeRef
@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class VurderOpptjeningsvilkårSteg extends VurderOpptjeningsvilkårStegFelles {

    @Inject
    public VurderOpptjeningsvilkårSteg(BehandlingRepositoryProvider repositoryProvider,
            OpptjeningRepository opptjeningRepository,
            InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste) {
        super(repositoryProvider, opptjeningRepository, inngangsvilkårFellesTjeneste, BehandlingStegType.VURDER_OPPTJENINGSVILKÅR);
    }

    @Override
    protected List<OpptjeningAktivitet> mapTilOpptjeningsaktiviteter(MapTilOpptjeningAktiviteter mapper, OpptjeningsvilkårResultat oppResultat) {
        List<OpptjeningAktivitet> aktiviteter = new ArrayList<>();
        aktiviteter.addAll(mapper.map(oppResultat.getUnderkjentePerioder(), OpptjeningAktivitetKlassifisering.BEKREFTET_AVVIST));
        aktiviteter.addAll(mapper.map(oppResultat.getAntattGodkjentePerioder(), OpptjeningAktivitetKlassifisering.ANTATT_GODKJENT));
        aktiviteter.addAll(mapper.map(oppResultat.getBekreftetGodkjentePerioder(), OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT));
        aktiviteter.addAll(mapper.map(oppResultat.getAkseptertMellomliggendePerioder(), OpptjeningAktivitetKlassifisering.MELLOMLIGGENDE_PERIODE));
        return aktiviteter;
    }

    /**
     * Overstyr stegresultat og sett en frist dersom vi må vente på
     * opptjeningsopplysninger.
     */
    @Override
    protected BehandleStegResultat stegResultat(RegelResultat regelResultat) {
        BehandleStegResultat stegResultat = super.stegResultat(regelResultat);
        AksjonspunktDefinisjon apDef = AksjonspunktDefinisjon.AUTO_VENT_PÅ_OPPTJENINGSOPPLYSNINGER;

        if (regelResultat.getAksjonspunktDefinisjoner().contains(apDef)) {
            LocalDateTime frist = getVentPåOpptjeningsopplysningerFrist(regelResultat);
            return stegResultat.medAksjonspunktResultat(
                    AksjonspunktResultat.opprettForAksjonspunktMedFrist(apDef, Venteårsak.VENT_OPPTJENING_OPPLYSNINGER, frist));
        }
        return stegResultat;
    }

    @Override
    protected BehandleStegResultat stegResultatVilkårIkkeOppfylt(RegelResultat regelResultat, Behandling behandling) {
        return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VURDER_OPPTJENINGSVILKÅRET));
    }

    private LocalDateTime getVentPåOpptjeningsopplysningerFrist(RegelResultat regelResultat) {
        Optional<OpptjeningsvilkårResultat> resultat = regelResultat.getEkstraResultat(OPPTJENINGSVILKÅRET);
        LocalDate now = LocalDate.now();
        LocalDate fristDato = resultat.isPresent() ? resultat.get().getFrist() : now;
        return of(fristDato, LocalDateTime.now().toLocalTime());
    }
}
