import type { CoffeeItem } from "@com.example/coffee-shop-client";
import { getMenuItems } from "@/app";
import MenuItem from "@/components/MenuItem";

const Menu = async () => {
    const menuItems: CoffeeItem[] = await getMenuItems();

    return menuItems?.length !== 0 ? (
        <section>
            <div className="home_menu">
                {menuItems?.map((item: CoffeeItem) => (
                    <MenuItem
                        key={item.type}
                        {...{
                            coffeeType: item.type!,
                            coffeeDescription: item.description!,
                        }}
                    />
                ))}
            </div>
        </section>
    ) : (
        <div className="home_menu-error">
            <h2 className="text-xl font-bold">Could not get menu.</h2>
        </div>
    );
};

export default Menu;
