package no.nav.foreldrepenger.produksjonsstyring.sakogbehandling;

import java.util.List;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import contract.sob.dto.Aktoer;
import contract.sob.dto.Applikasjoner;
import contract.sob.dto.Avslutningsstatuser;
import contract.sob.dto.BehandlingAvsluttet;
import contract.sob.dto.BehandlingOpprettet;
import contract.sob.dto.BehandlingStatus;
import contract.sob.dto.Behandlingstemaer;
import contract.sob.dto.Behandlingstyper;
import contract.sob.dto.PrimaerBehandling;
import contract.sob.dto.PrimaerRelasjonstyper;
import contract.sob.dto.Sakstemaer;
import no.nav.foreldrepenger.behandlingslager.behandling.Tema;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.kafka.JsonObjectMapper;
import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.kafka.SakOgBehandlingHendelseProducer;
import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.kafka.SakOgBehandlingHendelseProducerFeil;
import no.nav.vedtak.log.mdc.MDCOperations;

@Dependent
public class SakOgBehandlingTjeneste {

    private SakOgBehandlingAdapter adapter;
    private SakOgBehandlingHendelseProducer producer;

    public SakOgBehandlingTjeneste(){
        //for CDI
    }

    @Inject
    public SakOgBehandlingTjeneste(SakOgBehandlingAdapter adapter,
                                   SakOgBehandlingHendelseProducer producer) {
        this.adapter = adapter;
        this.producer = producer;
    }

    public void behandlingOpprettet(OpprettetBehandlingStatus status) {
        adapter.behandlingOpprettet(status);
    }

    public void behandlingAvsluttet(AvsluttetBehandlingStatus status) {
        adapter.behandlingAvsluttet(status);
    }

    public void behandlingStatusEndret(BehandlingStatusDto dto) {
        boolean erAvsluttet = dto.erBehandlingAvsluttet();

        var callId = MDCOperations.getCallId() != null ? MDCOperations.getCallId() : MDCOperations.generateCallId();

        var builder = erAvsluttet ? BehandlingAvsluttet.builder().avslutningsstatus(Avslutningsstatuser.builder().value("ok").build()) : BehandlingOpprettet.builder();

        builder.sakstema(Sakstemaer.builder().value(Tema.FOR.getOffisiellKode()).build())
            .behandlingstema(Behandlingstemaer.builder().value(dto.getBehandlingTema().getOffisiellKode()).build())
            .behandlingstype(Behandlingstyper.builder().value(dto.getBehandlingType().getOffisiellKode()).build())
            .behandlingsID(createUniqueBehandlingsId(String.valueOf(dto.getBehandlingId())))
            .aktoerREF(List.of(new Aktoer(dto.getAktørId().getId())))
            .ansvarligEnhetREF(dto.getEnhet().getEnhetId())
            .applikasjonSakREF(dto.getSaksnummer().getVerdi())
            .applikasjonBehandlingREF(dto.getBehandlingEksternRef().toString())
            .hendelsesId(callId)
            .hendelsesprodusentREF(Applikasjoner.builder().value(Fagsystem.FPSAK.getOffisiellKode()).build())
            .hendelsesTidspunkt(dto.getHendelsesTidspunkt());

        if (dto.getOriginalBehandlingId() != null) {
            builder.primaerBehandlingREF(new PrimaerBehandling(createUniqueBehandlingsId(String.valueOf(dto.getOriginalBehandlingId())),
                PrimaerRelasjonstyper.builder().value("forrige").build()));
        }

        producer.sendJsonMedNøkkel(createUniqueKey(String.valueOf(dto.getBehandlingId()), dto.getBehandlingStatusKode()), generatePayload(builder.build()));
    }

    private String createUniqueBehandlingsId(String behandlingsId) {
        return String.format("%s_%s", Fagsystem.FPSAK.getOffisiellKode(), behandlingsId);
    }

    private String createUniqueKey(String behandlingsId, String event) {
        return String.format("%s_%s_%s", Fagsystem.FPSAK.getOffisiellKode(), behandlingsId, event);
    }

    private String generatePayload(BehandlingStatus hendelse) {
        return JsonObjectMapper.toJson(hendelse, SakOgBehandlingHendelseProducerFeil.FACTORY::kanIkkeSerialisere);
    }

}
