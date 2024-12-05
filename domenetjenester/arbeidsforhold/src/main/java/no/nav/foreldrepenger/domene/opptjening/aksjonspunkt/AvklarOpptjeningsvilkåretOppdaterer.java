package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.domene.opptjening.dto.AvklarOpptjeningsvilkåretDto;
import no.nav.vedtak.exception.FunksjonellException;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarOpptjeningsvilkåretDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarOpptjeningsvilkåretOppdaterer implements AksjonspunktOppdaterer<AvklarOpptjeningsvilkåretDto> {

    private OpptjeningRepository opptjeningRepository;
    private Historikkinnslag2Repository historikkinnslagRepository;

    AvklarOpptjeningsvilkåretOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarOpptjeningsvilkåretOppdaterer(OpptjeningRepository opptjeningRepository,
                                               Historikkinnslag2Repository historikkinnslagRepository) {

        this.opptjeningRepository = opptjeningRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    @Override
    public OppdateringResultat oppdater(AvklarOpptjeningsvilkåretDto dto, AksjonspunktOppdaterParameter param) {
        var nyttUtfall = dto.getErVilkarOk() ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT;
        lagHistorikkInnslag(param, nyttUtfall, dto.getBegrunnelse());

        if (VilkårUtfallType.OPPFYLT.equals(nyttUtfall)) {
            sjekkOmVilkåretKanSettesTilOppfylt(param.getBehandlingId());
            return new OppdateringResultat.Builder()
                .leggTilManueltOppfyltVilkår(VilkårType.OPPTJENINGSVILKÅRET)
                .build();
        }
        return OppdateringResultat.utenTransisjon()
            .medFremoverHopp(FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR)
            .leggTilManueltAvslåttVilkår(VilkårType.OPPTJENINGSVILKÅRET, Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING)
            .build();

    }

    private void sjekkOmVilkåretKanSettesTilOppfylt(Long behandlingId) {
        var ant = opptjeningRepository.finnOpptjening(behandlingId).map(Opptjening::getOpptjeningAktivitet).orElse(List.of()).stream()
            .filter(oa -> !oa.getAktivitetType().equals(OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD))
            .count();
        if (ant > 0) {
            return;
        }
        throw new FunksjonellException("FP-093922", "Kan ikke sette opptjeningsvilkåret til oppfylt."
            + " Det må være minst en aktivitet for at opptjeningsvilkåret skal kunne settets til oppfylt.",
            "Sett på vent til det er mulig og manuelt legge inn aktiviteter ved overstyring.");
    }

    private void lagHistorikkInnslag(AksjonspunktOppdaterParameter param, VilkårUtfallType nyVerdi, String begrunnelse) {
        var ref = param.getRef();
        var historikkinnslag = new Historikkinnslag2.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(ref.fagsakId())
            .medBehandlingId(ref.behandlingId())
            .medTittel(SkjermlenkeType.PUNKT_FOR_OPPTJENING)
            .addLinje(fraTilEquals("Opptjeningsvilkåret", null, nyVerdi))
            .addLinje(begrunnelse)
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }
}
