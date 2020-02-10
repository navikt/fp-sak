package no.nav.foreldrepenger.web.app.tjenester.behandling.oppdrag;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Grad170;

public class Grad170Dto extends SporingDto {

    private String typeGrad;
    private int grad;

    public Grad170Dto(Grad170 entitet) {
        super(entitet, entitet.getVersjon(), entitet.getId());
    }

    public String getTypeGrad() {
        return typeGrad;
    }

    public void setTypeGrad(String typeGrad) {
        this.typeGrad = typeGrad;
    }

    public int getGrad() {
        return grad;
    }

    public void setGrad(int grad) {
        this.grad = grad;
    }

    public static Grad170Dto fraDomene(Grad170 grad170) {
        Grad170Dto grad170Dto = new Grad170Dto(grad170);
        grad170Dto.typeGrad = grad170.getTypeGrad();
        grad170Dto.grad = grad170.getGrad();
        return grad170Dto;
    }
}
