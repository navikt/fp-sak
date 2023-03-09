package no.nav.foreldrepenger.domene.opptjening.dto;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.utlanddok.OpptjeningIUtlandDokStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.UtlandMarkering;

public record OpptjeningIUtlandDokStatusDto(OpptjeningIUtlandDokStatus dokStatus, UtlandMarkering utlandMarkering) {

}
