package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.UtlandDokumentasjonStatus;
import no.nav.foreldrepenger.domene.opptjening.dto.MerkOpptjeningUtlandDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = MerkOpptjeningUtlandDto.class, adapter = AksjonspunktOppdaterer.class)
public class MerkOpptjeningUtlandOppdaterer implements AksjonspunktOppdaterer<MerkOpptjeningUtlandDto> {

    private FagsakEgenskapRepository fagsakEgenskapRepository;
    private HistorikkTjenesteAdapter historikkTjenesteAdapter;

    @Inject
    public MerkOpptjeningUtlandOppdaterer(FagsakEgenskapRepository fagsakEgenskapRepository,
                                          HistorikkTjenesteAdapter historikkAdapter) {
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
        this.historikkTjenesteAdapter = historikkAdapter;
    }

    MerkOpptjeningUtlandOppdaterer() {
        // CDI
    }

    @Override
    public OppdateringResultat oppdater(MerkOpptjeningUtlandDto dto, AksjonspunktOppdaterParameter param) {
        var eksisterende = fagsakEgenskapRepository.finnUtlandDokumentasjonStatus(param.getRef().fagsakId()).orElse(null);
        fagsakEgenskapRepository.lagreUtlandDokumentasjonStatus(param.getRef().fagsakId(), dto.getDokStatus());
        if (!Objects.equals(eksisterende, dto.getDokStatus())) {
            historikkTjenesteAdapter.tekstBuilder()
                .medEndretFelt(HistorikkEndretFeltType.INNHENT_SED, fraUtlandDokStatus(eksisterende), fraUtlandDokStatus(dto.getDokStatus()))
                .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret());
        }
        return OppdateringResultat.utenOverhopp();
    }

    private String fraUtlandDokStatus(UtlandDokumentasjonStatus status) {
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
