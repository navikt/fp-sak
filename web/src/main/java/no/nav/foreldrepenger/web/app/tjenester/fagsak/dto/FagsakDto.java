package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.web.app.rest.ResourceLink;

public class FagsakDto {  // SøkDto saksnummer + fagsakYtelseType + status + NavRestTjeneste
    private String saksnummer;
    private String saksnummerString;
    private FagsakYtelseType sakstype;
    private FagsakYtelseType fagsakYtelseType;
    private RelasjonsRolleType relasjonsRolleType;
    private FagsakStatus status;
    private LocalDate barnFodt;
    private Integer dekningsgrad;
    private String aktoerId;

    private List<ResourceLink> links = new ArrayList<>();
    private List<ResourceLink> onceLinks = new ArrayList<>();

    public FagsakDto() {
        // Injiseres i test
    }

    public FagsakDto(Fagsak fagsak,
                     LocalDate barnFodt,
                     RelasjonsRolleType relasjonsRolleType,
                     Integer dekningsgrad,
                     List<ResourceLink> links,
                     List<ResourceLink> linksOnce) {
        this.saksnummer = fagsak.getSaksnummer().getVerdi();
        this.saksnummerString = fagsak.getSaksnummer().getVerdi();
        this.aktoerId = fagsak.getAktørId().getId();
        this.sakstype = fagsak.getYtelseType();
        this.fagsakYtelseType = fagsak.getYtelseType();
        this.status = fagsak.getStatus();
        this.barnFodt = barnFodt;
        this.relasjonsRolleType = relasjonsRolleType;
        this.dekningsgrad = dekningsgrad;
        this.links = links;
        this.onceLinks = linksOnce;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public FagsakYtelseType getSakstype() {
        return sakstype;
    }

    public FagsakStatus getStatus() {
        return status;
    }

    public String getSaksnummerString() {
        return saksnummerString;
    }

    public LocalDate getBarnFodt() {
        return barnFodt;
    }

    public FagsakYtelseType getFagsakYtelseType() {
        return fagsakYtelseType;
    }

    public RelasjonsRolleType getRelasjonsRolleType() {
        return relasjonsRolleType;
    }

    public Integer getDekningsgrad() {
        return dekningsgrad;
    }

    public String getAktoerId() {
        return aktoerId;
    }

    public List<ResourceLink> getLinks() {
        return links;
    }

    public List<ResourceLink> getOnceLinks() {
        return onceLinks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var fagsakDto = (FagsakDto) o;
        return Objects.equals(saksnummer, fagsakDto.saksnummer)
            && Objects.equals(saksnummerString, fagsakDto.saksnummerString)
            && sakstype == fagsakDto.sakstype && relasjonsRolleType == fagsakDto.relasjonsRolleType && status == fagsakDto.status
            && Objects.equals(barnFodt, fagsakDto.barnFodt)
            && Objects.equals(dekningsgrad, fagsakDto.dekningsgrad)
            && Objects.equals(aktoerId, fagsakDto.aktoerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(saksnummer, saksnummerString, sakstype, relasjonsRolleType, status,
            barnFodt, dekningsgrad, aktoerId);
    }

    @Override
    public String toString() {
        return "FagsakDto{" +
            "saksnummer=" + saksnummer +
            ", sakstype=" + sakstype +
            ", relasjonsRolleType=" + relasjonsRolleType +
            ", status=" + status +
            ", barnFodt=" + barnFodt +
            ", dekningsgrad=" + dekningsgrad +
            '}';
    }
}
