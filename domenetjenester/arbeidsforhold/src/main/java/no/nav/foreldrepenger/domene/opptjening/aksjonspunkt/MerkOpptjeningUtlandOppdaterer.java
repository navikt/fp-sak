package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder.fraTilEquals;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.UtlandDokumentasjonStatus;
import no.nav.foreldrepenger.domene.opptjening.dto.MerkOpptjeningUtlandDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = MerkOpptjeningUtlandDto.class, adapter = AksjonspunktOppdaterer.class)
public class MerkOpptjeningUtlandOppdaterer implements AksjonspunktOppdaterer<MerkOpptjeningUtlandDto> {

    private FagsakEgenskapRepository fagsakEgenskapRepository;
    private Historikkinnslag2Repository historikkinnslagRepository;

    @Inject
    public MerkOpptjeningUtlandOppdaterer(FagsakEgenskapRepository fagsakEgenskapRepository,
                                          Historikkinnslag2Repository historikkinnslagRepository) {
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    MerkOpptjeningUtlandOppdaterer() {
        // CDI
    }

    @Override
    public OppdateringResultat oppdater(MerkOpptjeningUtlandDto dto, AksjonspunktOppdaterParameter param) {
        var ref = param.getRef();
        var fagsakId = ref.fagsakId();
        var eksisterende = fagsakEgenskapRepository.finnUtlandDokumentasjonStatus(fagsakId).orElse(null);
        fagsakEgenskapRepository.lagreUtlandDokumentasjonStatus(fagsakId, dto.getDokStatus());
        if (!Objects.equals(eksisterende, dto.getDokStatus())) {
            lagHistorikkinnslag(ref, dto, eksisterende);
        }
        return OppdateringResultat.utenOverhopp();
    }

    private void lagHistorikkinnslag(BehandlingReferanse ref, MerkOpptjeningUtlandDto dto, UtlandDokumentasjonStatus eksisterende) {
        var historikkinnslag = new Historikkinnslag2.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(ref.fagsakId())
            .medBehandlingId(ref.behandlingId())
            .medTittel("Fakta endret")
            .addTekstlinje(fraTilEquals("Innhent dokumentasjon", fraUtlandDokStatus(eksisterende), fraUtlandDokStatus(dto.getDokStatus())))
            .addTekstlinje(dto.getBegrunnelse())
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    private static String fraUtlandDokStatus(UtlandDokumentasjonStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case DOKUMENTASJON_VIL_IKKE_BLI_INNHENTET -> "Ikke innhent dokumentasjon";
            case DOKUMENTASJON_VIL_BLI_INNHENTET -> "Innhent dokumentasjon";
            case DOKUMENTASJON_ER_INNHENTET -> "Dokumentasjon er innhentet";
        };
    }
}
